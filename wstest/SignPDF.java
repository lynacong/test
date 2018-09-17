package ebill.service.service.impl;

import assp.evoucher.common.adapter.SignAndDEnvelopeAdaptHandler;
import assp.evoucher.common.util.Arithmetic;
import assp.evoucher.common.util.EVoucherException;
import assp.evoucher.common.util.ExceptionConstant;
import com.ctjsoft.util.exception.EBillVoucherException;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.AcroFields.Item;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;



/**
 * PDF 签名类  通过itextpdf来对pdf文件进行签名
 * @author LYN 
 */
public class SignPDF {
	
	private static final Logger logger = Logger.getLogger(SignPDF.class);

	private static  String certInfo;

	public static String getCertInfo() {
		return certInfo;
	}

	public static void setCertInfo(String certInfo) {
		SignPDF.certInfo = certInfo;
	}

	/**
	 * 指定字符位置PDF签名
	 * @param pdfData  pdf信息encode后的数据
	 * @param stampLocation  签章位置信息
	 * @param signaturePic  签章图片信息
	 * @return
	 */
	public static String pdfSignByText(String admDivCode, String pdfData, String stampLocation, byte[] signaturePic) {
		
		 
		String signPDFData = "";
		ByteArrayInputStream fillInputStream = null;
		PdfReader reader = null;
		ByteArrayOutputStream fout = null;
		PdfStamper stp = null;
		InputStream data = null;
		ByteArrayInputStream bais = null;
		try {
			//将pdf转换成流数据
			fillInputStream = new ByteArrayInputStream(Arithmetic.decodeForByte(pdfData));
			//读取数据流
			reader = new PdfReader(fillInputStream);
			fout = new ByteArrayOutputStream();
			//创建签名域 
			stp = PdfStamper.createSignature(reader, fout, '\0',null, true);

			PdfSignatureAppearance sap = stp.getSignatureAppearance();
			sap.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED); 
			
			String SignatureFieldName = SignPDF.searchSignatureField(reader);
			Object[] obj = new Object[2];
			if("".equals(stampLocation)){
				obj[0] = 100;
				obj[1] = 100;
			}else if("1".equals(stampLocation)){
				obj[0] = 230;
				obj[1] = 290;
				//obj = JasportPDF.getKeyWords(pdfData, stampLocation);
			}else if("2".equals(stampLocation)){
				obj[0] = 50;
				obj[1] = 40;
				//obj = JasportPDF.getKeyWords(pdfData, stampLocation);
			}else{
				throw new EBillVoucherException("未找到签章位置");
			}
			//下面的是设置图片信息和证书信息
			float x = Float.parseFloat(obj[0].toString()) + 10;
			float y = Float.parseFloat(obj[1].toString());
			Image img = Image.getInstance(signaturePic);
			sap.setVisibleSignature(new Rectangle(x, y-30, x+100, y+30), 1, SignatureFieldName);//llx =left, ury = top
			sap.setSignatureGraphic(img);
			sap.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
			PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));
			dic.setReason(sap.getReason());
			dic.setLocation(sap.getLocation());
			dic.setContact(sap.getContact());
			dic.setDate(new PdfDate(sap.getSignDate()));
			sap.setCryptoDictionary(dic);
			int contentEstimated = 7000;
			HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
			exc.put(PdfName.CONTENTS, new Integer(contentEstimated * 2 + 2));
			sap.setReason("");
			sap.setLocation("");
			sap.preClose(exc);
			data = sap.getRangeStream();
			MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
			byte buf[] = new byte[8192];
			int n;
			while ((n = data.read(buf)) > 0) {
				messageDigest.update(buf, 0, n);
			}
			byte hash[] = messageDigest.digest();

			Calendar cal = Calendar.getInstance();
		  //JD的签名验签服务器没有找到直接取证书的地方  只有先签个名然后在取个证书才能够取到
			if (certInfo==null) {
				certInfo = SignAndDEnvelopeAdaptHandler.getCert(admDivCode, "VS", 7, "");
				SignPDF.setCertInfo(certInfo);
			}

			byte[] bt = Arithmetic.decodeForByte(certInfo);
			bais = new ByteArrayInputStream(bt);
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Collection c = cf.generateCertificates(bais);
			Iterator i = c.iterator();
			Certificate[] chain = new Certificate[c.size()];
			int flag = 0;
			while (i.hasNext()) {
			    Certificate cert = (Certificate)i.next();
			    chain[flag] = cert;
			    flag++;
			 }
			PdfPKCS7 pk7 = new PdfPKCS7(null, chain, "SHA1", null, null, false);
			byte[] sh = pk7.getAuthenticatedAttributeBytes(hash, cal, null, null,null);
		
			String result = SignAndDEnvelopeAdaptHandler.p1Sign(sh,admDivCode);
			pk7.setExternalDigest(Arithmetic.decodeForByte(result), null, "RSA");

			byte[] sg = pk7.getEncodedPKCS7(hash, cal);
			
			if (contentEstimated + 2 < sg.length)
				throw new DocumentException("Not enough space");
			byte[] paddedSig = new byte[contentEstimated];
		    System.arraycopy(sg, 0, paddedSig, 0, sg.length);
			PdfDictionary dic2 = new PdfDictionary();
			dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
			sap.close(dic2); 
			
			signPDFData = Arithmetic.encode(fout.toByteArray());
			
		}catch (IOException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			logger.error(e.getMessage());
		} catch (NoSuchProviderException e) {
			logger.error(e.getMessage());
		} catch (EVoucherException e){
			logger.error(e.getMessage());
			throw e;		
		}catch (Exception e){
			logger.error(e.getMessage());
			throw new EVoucherException(ExceptionConstant.EVS414 + e.getMessage(), e);			
		}
		finally{
			try {
				if(bais != null)
					bais.close();
				if(data != null)
					data.close();
				if(stp != null)
					stp.close();
				if(fout != null)
					fout.close();
				if(reader != null)
					reader.close();
				if(fillInputStream != null)
					fillInputStream.close();
			} catch (Exception e) {
				logger.error("PDF签名流关闭失败, cause by: " + e.getMessage(), e);
			}
		}
		return signPDFData;
	}
	
	private static String searchSignatureField(PdfReader reader){
		AcroFields fileds = reader.getAcroFields();
		Map<String, Item> map = fileds.getFields();
		Set<String> set = map.keySet();
		Iterator<String> iterator = set.iterator();
		String signFiledName = "SignatureField0";
		while(iterator.hasNext()){
			String key = iterator.next().toString();
			if(key.compareTo(signFiledName)>0)
				signFiledName = key;
		}
		int signFlag = Integer.parseInt(signFiledName.substring(14,signFiledName.length())) + 1;
		return "SignatureField" + signFlag;
	}

	
}