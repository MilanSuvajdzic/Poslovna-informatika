package com.example.bank.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.model.AnalyticsOfStatements;
import com.example.bank.model.RealTimeGrossSettlement;
import com.example.bank.model.Bank;
import com.example.bank.service.AnalyticsOfStatementsService;
import com.example.bank.service.DailyAccountBalanceService;
import com.example.bank.service.IRealTimeGrossSettlementService;
import com.example.bank.service.impl.BankServiceImpl;

@RestController
@CrossOrigin(origins="*")
@RequestMapping("/public/analytics")
public class AnalyticsController {

	@Autowired
	private AnalyticsOfStatementsService service;
	
	@Autowired
	private BankServiceImpl bankService;
	
	@Autowired
	private DailyAccountBalanceService dailyAccountBalanceService;
	
	@Autowired
	private IRealTimeGrossSettlementService realTimeGrossSettlementService;

	@RequestMapping(
			value = "/load", 	
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public List<AnalyticsOfStatements>loadAnalytics(@RequestBody String putanjaClient) {		
		
		/*List<AnalyticsOfStatements> analitike = service.findAll();
				
		for(AnalyticsOfStatements analitika: analitike){
			
			String xmlString = "";
		    try {
		        JAXBContext context = JAXBContext.newInstance(AnalyticsOfStatements.class);
		        Marshaller m = context.createMarshaller();

		        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // To format XML

		        StringWriter sw = new StringWriter();
		        m.marshal(analitika, sw);
		        xmlString = sw.toString();

		    } catch (JAXBException e) {
		        e.printStackTrace();
		    }
		    System.out.println(xmlString);		
		    
		    BufferedWriter bw = null;
			FileWriter fw = null;
		    
		    String putanja = "Analytic-" + analitika.getItemNumber().toString() +".xml";
		    try{
		    	//f = new File("C:\\filename.txt");
		    	fw = new FileWriter("E:\\Analytics\\" + putanja);		    	
		    	bw = new BufferedWriter(fw);
		    	bw.write(xmlString);
		    	
		    } catch (IOException ex) {

				ex.printStackTrace();

			} finally {
				try {

					if (bw != null)
						bw.close();

					if (fw != null)
						fw.close();					
						

				} catch (IOException ex) {

					ex.printStackTrace();

				}
			}
		 
		} */
		
		System.out.println("PUTANJA JE: " + putanjaClient);
			
		
		File[] files = new File(putanjaClient).listFiles();
		//If this pathname does not denote a directory, then listFiles() returns null. 
		long time = System.currentTimeMillis();
		Date current = new Date(time);

		service.deleteAll();
		for (File file : files) {
		    
		    	JAXBContext jaxbContext;
				try {
					jaxbContext = JAXBContext.newInstance(AnalyticsOfStatements.class);
					Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			    	AnalyticsOfStatements analyticParsed = (AnalyticsOfStatements) jaxbUnmarshaller.unmarshal(file);
			    	
//			    	analyticParsed.setDateOfReceipt(current);
			    //	analyticParsed.setCurrencyDate(current);
			    	
			    	//updateDailyAccountBalance(analyticParsed);
			    	
			    	
			    	klasifikujAnalitiku(analyticParsed);
			    	service.save(analyticParsed);
//			    	//updateDailyAccountBalance(analyticParsed);
				} catch (JAXBException e) {					 
					e.printStackTrace();
				}
		    	
		}
		List<AnalyticsOfStatements> analitike = service.findAll(); 
		return service.findAll();
		//return null;
		
	}
	
	@DeleteMapping
	public void deleteAnalytics() {
		
		System.out.println("POGODIO BRISANJE");
		
		List<AnalyticsOfStatements> analytics = service.findAll();
		
		for(AnalyticsOfStatements a: analytics){
			service.delete(a.getItemNumber());
		}
	}
	

	public void klasifikujAnalitiku(AnalyticsOfStatements analytics) {
		
		String currentBank = "555";
		
		
		if (analytics.getAccountCreditor().substring(0,  3).equals(currentBank) && analytics.getDebtorAccount() == null) { //uplata na racun
			
			//updateDailyAccountBalance(analytics);
			
			service.save(analytics);
			
		} else if (analytics.getAccountCreditor() == null && analytics.getDebtorAccount().substring(0,  3).equals(currentBank)) { //isplata
			
			//updateDailyAccountBalance(analytics);
			
			service.save(analytics);
			
		} else if (analytics.getAccountCreditor().substring(0,  3).equals(currentBank) && analytics.getDebtorAccount().substring(0,  3).equals(currentBank)) { //unutarbankarski transfer
			
			
			AnalyticsOfStatements analyticsCredit = analytics;
			analyticsCredit.setDebtorAccount(null);
			
			AnalyticsOfStatements analyticsDebt = analytics;
			analyticsDebt.setAccountCreditor(null);
			
			//updateDailyAccountBalance(analyticsCredit);
			//updateDailyAccountBalance(analyticsDebt);
			
			service.save(analyticsCredit);
			service.save(analyticsDebt);
			
		} else if (analytics.getDebtorAccount().substring(0,  3).equals(currentBank) && !analytics.getAccountCreditor().substring(0,  3).equals(currentBank)) { //medjubankarski transfer
			

			//updateDailyAccountBalance(analytics);
			
			service.save(analytics);
			
			generateInterbankTransfer(analytics);
		}
	}
	
	public void generateInterbankTransfer(AnalyticsOfStatements analytics) {
		
		String currentBankSwift = "55555555";
		String obracunskiRacunBankeDuznika = "555989898989812345";
		
		if (analytics.getSum() < 250000 && !analytics.getEmergency()) {
			
			
		} else if (analytics.getSum() >= 250000 || analytics.getEmergency()) { //generisanje rtgs-a
			
			Random rand = new Random();
			int porukaID = rand.nextInt(1000) + 1;
			
			List<Bank> banks = bankService.getAll();
			
			
			
			RealTimeGrossSettlement rtgs = new RealTimeGrossSettlement();
			rtgs.setPorukaID(porukaID + "");
			rtgs.setDuznikSWIFT(currentBankSwift);
			rtgs.setDuznikObracunskiRacun(obracunskiRacunBankeDuznika);
			
			for (Bank bank : banks) {
				
				if (bank.getRacun().substring(0, 3).equals(analytics.getAccountCreditor().substring(0, 3))) {
					
					rtgs.setPoverilacSWIFT(bank.getSwift());
					rtgs.setPoveriocObracunskiRacun(bank.getRacun());
					break;
				}
			}
			
			rtgs.setDuznik(analytics.getDebtor_originator());
			rtgs.setSvrhaPlacanja(analytics.getPurposeOfPayment());
			rtgs.setPoverilac(analytics.getCreditor_recipient());
			rtgs.setDatumNaloga(analytics.getDateOfReceipt());
			rtgs.setDatumValute(analytics.getCurrencyDate());
			rtgs.setDuznikRacun(analytics.getDebtorAccount());
			rtgs.setModelZaduzenja(new Long(analytics.getModelAssigments()));
			rtgs.setPozivNaBrojZaduzenja(analytics.getReferenceNumberAssigments());
			rtgs.setPoverlacRacun(analytics.getAccountCreditor());
			rtgs.setModelOdobrenja(new Long(analytics.getModelApproval()));
			rtgs.setPozivNaBrojOdobrenja(analytics.getReferenceNumberCreditor());
			rtgs.setIznos(analytics.getSum());
			rtgs.setSifraValute(analytics.getPaymentCurrency().getOfficial_code());
			
			realTimeGrossSettlementService.registerRTGS(rtgs);
		}
	}

	private void updateDailyAccountBalance(AnalyticsOfStatements analytic) {
		dailyAccountBalanceService.update(analytic);
	}
	
	public void exportAccountStatement() {

		
	}
	
}
