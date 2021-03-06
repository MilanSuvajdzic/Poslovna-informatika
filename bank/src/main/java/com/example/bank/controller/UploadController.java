package com.example.bank.controller;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.example.bank.model.AnalyticsOfStatements;
import com.example.bank.service.AnalyticsOfStatementsService;
import com.example.bank.service.DailyAccountBalanceService;
import com.example.bank.service.StorageService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/public/upload")
public class UploadController {
	
	private String currentBank="555";
	
	@Autowired
	StorageService storageService;
	
	@Autowired
	private DailyAccountBalanceService dailyAccountBalanceService;
	
	@Autowired
	private AnalyticsOfStatementsService service;
	
	List<String> files = new ArrayList<String>();
	
	@PostMapping("/post")
	public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file){
		String message="";
		try{
			
			Path path=storageService.store(file);
			files.add(file.getOriginalFilename());
			AnalyticsOfStatements analyticParsed=storageService.loadAnalyticOfStatements(path);

			dailyAccountBalanceService.klasifikujAnalitiku(analyticParsed);

			message = "You successfully uploaded " + file.getOriginalFilename() + "!";
			return ResponseEntity.status(HttpStatus.OK).body(message);			
		}catch(Exception e){
			message = "FAIL to upload " + file.getOriginalFilename() + "!";
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
		}				
	}
	
	
	@GetMapping("/getallfiles")
	public ResponseEntity<List<String>> getListFiles(Model model) {
		List<String> fileNames = files.stream().map(fileName -> MvcUriComponentsBuilder
				.fromMethodName(UploadController.class, "getFile", fileName).build().toString())
				.collect(Collectors.toList());
				
		return ResponseEntity.ok().body(fileNames);
	}
	
	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> getFile(@PathVariable String filename) {
		Resource file = storageService.loadFile(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}	
	

}
