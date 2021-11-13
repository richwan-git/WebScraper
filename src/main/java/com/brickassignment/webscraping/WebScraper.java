/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.brickassignment.webscraping;
import static com.brickassignment.webscraping.XMLParser.getConfig;
import static com.brickassignment.webscraping.XMLParser.loadConfig;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import org.openqa.selenium.support.ui.WebDriverWait;
/**
 *
 * @author rafii
 */
public class WebScraper {
    private final static String defaultPaginationClass = "css-txlndr-unf-pagination";
    private final static String defaultProductClass = "e1nlzfl3";
    private final static String defaultPromotedProductClass = "css-1f8sh1y";
    private final static String defaultNameOfProductClass = "css-1bjwylw";
    private final static String defaultPriceOfProductClass = "css-o5uqvq";
    private final static String defaultImageOfProductClass = "css-t8frx0";
    private final static String defaultReviewCountOfProductClass = "css-153qjw7";
    private final static String defaultSelectImageOfProduct = "div[class=css-1b60o1a]";
    private final static String defaultNameOfMerchantClass = "css-1kr22w3";
    private final static String defaultSelectDescriptionOfProduct = "span[class=css-168ydy0 e1iszlzh1]";
    private final static String defaultSelectRatingOfProduct = "meta[itemprop=ratingValue]";
    
    public static void main(String[] args) {
        org.w3c.dom.Document configDoc = loadConfig();
        try {
            System.setProperty("java.classpath", ".");
            System.setProperty("webdriver.edge.driver",".\\driver\\edgedriver\\msedgedriver.exe");
            WebDriver driver = new EdgeDriver();
            WebDriverWait wait = new WebDriverWait(driver, new Long("100"));
            
            int page = 0;
            int idxProd = 0;
            String filename = getConfig(configDoc, "filename", "web parsing").concat(".csv");
            System.out.printf("Writing into : %s\n", filename);
            new File(".\\output").mkdirs();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(new File(".\\output\\"+filename)));
            csvWriter.writeNext(new String[]{
                "No",
                "Name of Product", 
                "Description", 
                "Image Link", 
                "Price", 
                "Rating (out of 5 stars)", 
                "Name of store of merchant",
                "Total Review",
            });
            try{
                int expectedProductCount = 100;
                while (idxProd < expectedProductCount) {
                    page++;
                    String searchLink = "https://www.tokopedia.com/p/handphone-tablet/handphone?page=" + page + "&ob=5";
                    driver.get(searchLink);
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("window.scrollBy(0,350)", "");
                    wait.until(presenceOfElementLocated(By.className("css-txlndr-unf-pagination")));
                    String html_content = driver.getPageSource();
                    Document doc = Jsoup.parse(html_content);
                    System.out.printf("Reading page %s, Title: %s\n", page, doc.title());

                    Elements products = doc.body().getElementsByClass(getConfig(configDoc, "product-classname", defaultProductClass));
                    for (Element product : products) {
                        if (product.getElementsByClass(getConfig(configDoc, "promoted-product-classname", defaultPromotedProductClass)).outerHtml().isEmpty()) {
                            String productName = product.getElementsByClass(getConfig(configDoc, "product-name-classname", defaultNameOfProductClass)).text();
                            String productPrice = product.getElementsByClass(getConfig(configDoc, "product-price-classname", defaultPriceOfProductClass)).text();
                            String productImage = product.getElementsByClass(getConfig(configDoc, "product-image-classname", defaultImageOfProductClass)).select("img").attr("src");
                            String storeMerchant = product.getElementsByClass(getConfig(configDoc, "merchant-name-classname", defaultNameOfMerchantClass)).get(1).text();
                            String reviewCount = StringUtils.substringBetween(product.getElementsByClass(getConfig(configDoc, "product-review-count-classname", defaultReviewCountOfProductClass)).first()
                                    .getElementsByTag("span").text(), "(", ")");

                            String additionalLink = product.select("a").attr("href");
                            Document docproduct = Jsoup.connect(additionalLink).userAgent("M").get();

                            Element ratingElement = docproduct.select(getConfig(configDoc, "product-rating-select", defaultSelectRatingOfProduct)).first();
                            String productRating = ratingElement.attr("content");

                            Element descElement = docproduct.select(getConfig(configDoc, "product-description-select", defaultSelectDescriptionOfProduct)).first();
                            String productDescription = descElement.text();

                            if (productImage.isEmpty()) {
                                Element imageElement = docproduct.select(getConfig(configDoc, "product-image-select", defaultSelectImageOfProduct)).first();
                                productImage = imageElement.select("img").attr("src");
                            }

                            String[] data = {String.valueOf(idxProd + 1),
                                productName,
                                productDescription,
                                productImage,
                                productPrice,
                                productRating,
                                storeMerchant,
                                reviewCount};
                            csvWriter.writeNext(data);

                            idxProd++;
                            if (idxProd == expectedProductCount) {
                                break;
                            }
                        }
                    }
                    System.out.println("Done reading page " + page);
                }

                csvWriter.close();
                driver.close();

                System.out.println("Total Produk : " + idxProd);
            } catch (Exception ex){
                driver.close();
                throw ex;
            }
        } catch (IOException ex) {
            Logger.getLogger(WebScraper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
