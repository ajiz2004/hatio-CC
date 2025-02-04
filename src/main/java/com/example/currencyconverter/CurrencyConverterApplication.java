package com.example.currencyconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;

@SpringBootApplication
public class CurrencyConverterApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurrencyConverterApplication.class, args);
    }
}

@RestController
@RequestMapping("/api")
class CurrencyController {
    private final CurrencyService currencyService;
    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping("/rates")
    public Map<String, Double> getRates(@RequestParam(defaultValue = "USD") String base) {
        return currencyService.getExchangeRates(base);
    }

    @PostMapping("/convert")
    public ConversionResponse convert(@RequestBody ConversionRequest request) {
        return currencyService.convertCurrency(request);
    }
}

@Service
class CurrencyService {
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Double> getExchangeRates(String base) {
        String url = API_URL + base;
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return (Map<String, Double>) response.get("rates");
    }

    public ConversionResponse convertCurrency(ConversionRequest request) {
        Map<String, Double> rates = getExchangeRates(request.getFrom());
        if (!rates.containsKey(request.getTo())) {
            throw new IllegalArgumentException("Invalid currency code: " + request.getTo());
        }
        double rate = rates.get(request.getTo());
        double convertedAmount = request.getAmount() * rate;
        return new ConversionResponse(request.getFrom(), request.getTo(), request.getAmount(), convertedAmount);
    }
}

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong: " + e.getMessage()));
    }
}

class ConversionRequest {
    private String from;
    private String to;
    private double amount;
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

class ConversionResponse {
    private String from;
    private String to;
    private double amount;
    private double convertedAmount;
    public ConversionResponse(String from, String to, double amount, double convertedAmount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.convertedAmount = convertedAmount;
    }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public double getAmount() { return amount; }
    public double getConvertedAmount() { return convertedAmount; }
}
