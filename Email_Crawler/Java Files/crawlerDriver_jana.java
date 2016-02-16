import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;


/*
 * @author Grace Miller
 * 2/15/2016 for Jana Coding Interview
 * 
 * This file takes in a url that has javascript interface rather than simply an html interface
 * It differs from the regular crawler by using selenium to automate a web browser so as to load
 * the javascript and run it to obtain the website's full html code.  
 * 
 * This takes in a url and then crawls over all of it's children pages, searching the html for 
 * any emails listed
 * 
 * @variable MAX_PAGE is the depth that the program searches for webpages, increase it to increase
 * depth
 * @varibale startURL- the base url to crawl over for emails
 * 
 */


public class crawlerDriver_jana{
	//change to increase depth reached
	private static int MAX_PAGES = 10;
	//set to ensure pages are not visited twice
	private static Set<String> pagesVisited= new HashSet<String>();
	//
	private static Queue<String> pagesToVisit= new ArrayDeque<String>();
	//private static String startURL;
	static String startURL = "http://www.jana.com";
	private static WebDriver driver;
	
	
	public static void main(String[] args) throws IOException{
		//Validate.isTrue(args.length == 1, "usage: supply url to fetch");
		///^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/
		//startURL = args[0];
		
		//TODO: need to change the path to the chromedriver to where on your computer
		System.setProperty("webdriver.chrome.driver", "/Users/gracemiller/Downloads/chromedriver");
		
		//start the web driver
        driver = new ChromeDriver();
        
		System.out.println("finding emails on this url: " + startURL);
		
		//add the first url into the pages to visit queue, crawl over the page
		Set<String> emails = crawl(startURL);
		
		//iterate through every email found
		Iterator<String> emailIterator = emails.iterator();
		while(emailIterator.hasNext()){
			System.out.println(emailIterator.next());
		}
		
		//close the driver
		driver.close();
		
		
	}
	
	/*
	 * private static Set<String> crawl(String nextURL)
	 * Main function that executes the crawling function of iterating over 
	 * the queue of pagesToVisit and visits each page while the depth has not been reached and
	 * there are still pages to search
	 * 
	 * @param nextURL- the base url to crawl the children off
	 * @return a set of emails found
	 */
	private static Set<String> crawl(String nextURL) throws IOException{
		pagesToVisit.add(nextURL);

		int pagesSearched = 0;
		Set<String> emailsFound = new HashSet<String>();

		//while the pages are not at maxpages
		if(pagesSearched < MAX_PAGES){
			//while there are still pages to visit
			
			while(!pagesToVisit.isEmpty()){
				//get the next url
				String currUrl = pagesToVisit.poll();
				//incrememnt the page count
				pagesSearched++;
				//get the emails from this url
				emailsFound = searchForEmail(emailsFound, currUrl);
				//search this for other urls
				getURLs(currUrl);
				//add this to the pagesVisited
				pagesVisited.add(currUrl);
		
			}//if no pages to visit then return the list of emails
		}//if the pages at maxpages then return the list of emails
		return emailsFound;
				
	}
	
	
	/*
	 * Set<String> searchForEmail(Set<String> emailsFound, String url)
	 * 
	 * Takes in a url and searches the html for any emails, then adds them
	 * to the email set
	 * 
	 * @param String url- url of the html to search
	 * @param Set<String> emailsFound- the email set to append to
	 * @return a set of emails found
	 */
	private static Set<String> searchForEmail(Set<String> emailsFound, String url) throws IOException{
		Set<String> emailList = emailsFound;
		
		//using the web driver get the html of the given url
        driver.get(url);
        String bodyText = driver.getPageSource();
        
        //regex of the standard email format
		Pattern pattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
		
		//find all instances matching the regex
		Matcher matcher = pattern.matcher(bodyText);
		while (matcher.find()) {
			//only add new emails
			String email = matcher.group();
			if(!emailList.contains(email)){
				emailList.add(email);
			}
		}

		return emailList;
	}
	
	
	/*
	 * void getURLs(String currURL)
	 * 
	 * given the current url this function crawls over all links in the html
	 * of the current url and adds any links found to the pagesToVisit queue
	 * 
	 * @param String currURL- url of the html to search
	 * @return void
	 */
	public static void getURLs(String currURL) throws IOException{
		try{
			//connect to the url page, pull the html from the page
			Connection connection = Jsoup.connect(currURL);		
			Document htmlDocument = connection.get();		
			//String bodyText = htmlDocument.body().text();
			
			//find all links declared using the href tag
			Elements linksOnPage = htmlDocument.select("a[href]");
			
			//for each link found
			  for(Element link : linksOnPage){
				  String currLink = link.absUrl("href");
				  //normalize the link to lowercase 
				  String normalizeLink = currLink.toLowerCase();
				  
				  //if the link has not been seen before and it is under the same
				  //domain as the first url
				  if(!pagesVisited.contains(normalizeLink) && currLink.startsWith(startURL)){
					  //add it to the pages to visit and the pages visited
					  pagesVisited.add(normalizeLink);
					  pagesToVisit.add(normalizeLink);
				  }
	          }
			  
			    
			 //used to now search the javascript of the website, find the extensions
			  //defined with the changeRoute in the span tag
			  Elements all= htmlDocument.select("span");
			  for(Element tempWord : all){
				  String suffix = tempWord.text();
				  //concat the extension to the end of the start link
				  String currLink = (startURL + "/" + suffix);
				  String normalizeLink = currLink.toLowerCase();
				  
				  //if the link not found, add it to be searched
				  if(!pagesVisited.contains(normalizeLink)){
					  pagesToVisit.add(normalizeLink);
					  pagesVisited.add(normalizeLink);
				  }
				  
			  }
		}catch (MalformedURLException mue) {
	         mue.printStackTrace();
	    }
		
	}
	
	
}