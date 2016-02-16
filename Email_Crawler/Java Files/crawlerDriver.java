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
import org.jsoup.helper.Validate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import org.openqa.selenium.chrome.ChromeDriverService;

/*
 * @author Grace Miller
 * 2/15/2016 for Jana Coding Interview
 * 
 * This class in a url and then crawls over all of it's children pages, searching the html for 
 * any emails listed
 * 
 * @variable MAX_PAGE is the depth that the program searches for webpages, increase it to increase
 * depth
 * @variable startURL- the base url to crawl over for emails
 * 
 */



public class crawlerDriver {
	
	private static int MAX_PAGES = 10;
	private static Set<String> pagesVisited= new HashSet<String>();
	private static Queue<String> pagesToVisit= new ArrayDeque<String>();
	static String startURL = "http://www.mit.edu";
	
	
	public static void main(String[] args) throws IOException{
		//uncomment for argument check on command line
		//Validate.isTrue(args.length == 1, "usage: supply url to fetch");
		
		//checks that the url is valid
		if(startURL.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")){
			System.out.println("finding emails on this url: " + startURL);
			
			//add the first url into the pages to visit queue
			Set<String> emails = crawl(startURL);
			
			//iterate over the list of emails 
			Iterator<String> emailIterator = emails.iterator();
			while(emailIterator.hasNext()){
				System.out.println(emailIterator.next());
			}
		}else{
			System.out.println("invalid url.  Please provide valid url");
		}
				
		
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
		
		
		try{ 
			//open up a connection to the current webpage, get the html of the page
			Connection nconnection = Jsoup.connect(url);
			Document htmlDocument = nconnection
					.ignoreHttpErrors(true)
					.timeout(0)
					.get();
	
			String bodyText = htmlDocument.toString();
		
	        //regex of the standard email format
			Pattern pattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
			
			//find all instances matching the regex
			Matcher matcher = pattern.matcher(bodyText);
			while (matcher.find()) {
				String email = matcher.group();
				//if the found email is not already in the email list, add it
				if(!emailList.contains(email)){
					emailList.add(email);
				}
			}
			return emailList;
		}
		catch (MalformedURLException mue) {
	         mue.printStackTrace();
	    }
		return null;
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
			Document htmlDocument = connection
					.ignoreHttpErrors(true)
					.timeout(0)
					.get();		
			
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