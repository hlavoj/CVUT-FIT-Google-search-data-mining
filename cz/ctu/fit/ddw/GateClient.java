
package cz.ctu.fit.ddw;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;



import org.jsoup.Jsoup;

import com.google.gson.Gson;


public class GateClient {
    
    // corpus pipeline
    private static SerialAnalyserController annotationPipeline = null;
    
    // whether the GATE is initialised
    private static boolean isGateInitilised = false;
    
    public GoogleResults googleSearch(String string) {
    	System.out.println("Searching query: " + string);
    	String google = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
        String search = string;
        String charset = "UTF-8";

        URL url;
		try {
			url = new URL(google + URLEncoder.encode(search, charset));
		
	        Reader reader = new InputStreamReader(url.openStream(), charset);
	        GoogleResults results = new Gson().fromJson(reader, GoogleResults.class);
	
	        return results;
	        // Show title and URL of 1st result.
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
    
    
    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }
    
    private final String USER_AGENT = "Mozilla/5.0";

	private String sendGet(String urlString) {

		String charset = "UTF-8";
		try {
			URL obj = new URL(URLDecoder.decode(urlString, charset));
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + urlString);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			
			String html = response.toString();
			String text = html2text(html);
			
			return text;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
    
    public void run(){
        
        if(!isGateInitilised){
            
            // initialise GATE
            initialiseGate();            
        }        

        try {                
            // create an instance of a Document Reset processing resource
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");

            // create an instance of a English Tokeniser processing resource
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");

            // create an instance of a Sentence Splitter processing resource
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");
            
            ProcessingResource GazetteerPR = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
            
            // locate the JAPE grammar file
            File japeOrigFile = new File("C:\\Users\\Vojta\\Desktop\\DDW\\jape-example.jape");
            java.net.URI japeURI = japeOrigFile.toURI();
            
            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                // set the grammar location
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                // set the grammar encoding
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }
            
            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            // create corpus pipeline
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            // add the processing resources (modules) to the pipeline
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(tokenizerPR);
            annotationPipeline.add(sentenceSplitterPR);
            annotationPipeline.add(GazetteerPR);
            annotationPipeline.add(japeTransducerPR);
            
            
            String allText = "";
            ArrayList<String> googleTen = new ArrayList<String>(10);
            GoogleResults results = googleSearch("most stupid people");
            for (int i = 0; i < results.getResponseData().getResults().size(); i++) {
            	String url = results.getResponseData().getResults().get(i).getUrl();
            	try{
            		String text = sendGet(url);
            		allText += " "+text;
            		googleTen.add(text);
            	}catch(Exception e){
            		
            	}

			}
            
            System.out.println("");
            
            Document document = Factory.newDocument(allText);

            // create a corpus and add the document
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);

            // set the corpus to the pipeline
            annotationPipeline.setCorpus(corpus);

            //run the pipeline
            annotationPipeline.execute();

            // loop through the documents in the corpus
            for(int i=0; i< corpus.size(); i++){

                Document doc = corpus.get(i);

                // get the default annotation set
                AnnotationSet as_default = doc.getAnnotations();

                FeatureMap futureMap = null;
                // get all Token annotations
                AnnotationSet annSetTokens = as_default.get("Country",futureMap);
                AnnotationSet annSetTokensFemale = as_default.get("PersonFemale",futureMap);
                AnnotationSet annSetTokensMale = as_default.get("PersonMale",futureMap);
                double country = annSetTokens.size();
                double female = annSetTokensFemale.size();
                double male =annSetTokensMale.size();
                
                double malePercent = (male /(male +female))*100;
                double femalePercent = (female /(male +female))*100;
                
                System.out.println("========= Male/Female statistic: ===========");
                System.out.println("Number of Token Female: " + (int)female + " - It is " + (int)femalePercent +"%");
                System.out.println("Number of Token Male: " + (int)male+ " - It is " + (int)malePercent +"%");

                ArrayList tokenAnnotations = new ArrayList(annSetTokens);
                
                ArrayList<String> list = new ArrayList<String>(20);
                
                // looop through the Token annotations
                for(int j = 0; j < tokenAnnotations.size(); ++j) {

                    // get a token annotation
                    Annotation token = (Annotation)tokenAnnotations.get(j);

                    // get the underlying string for the Token
                    Node isaStart = token.getStartNode();
                    Node isaEnd = token.getEndNode();
                    String underlyingString = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    list.add(underlyingString);
                   //  System.out.println("Token: " + underlyingString);
                    
                    // get the features of the token
                    FeatureMap annFM = token.getFeatures();
                    
                    // get the value of the "string" feature
                    //String value = (String)annFM.get((Object)"string");
                    //System.out.println("Token: " + value);
                }
                
                Map<String, Integer> map = new HashMap<String, Integer>();
                for (String temp2 : list) {
                	String temp = temp2.toLowerCase();
                	if(temp.toLowerCase().equals("uk"))
            			temp = "united kingdom";
            		if(temp.toLowerCase().equals("us"))
            			temp = "united states";
            		if(temp.toLowerCase().equals("usa"))
            			temp = "united states";
            		if(temp.toLowerCase().equals("u.s."))
            			temp = "united states";
            		if(temp.toLowerCase().equals("america"))
            			temp = "united states";
            		if(temp.toLowerCase().equals("amerika"))
            			temp = "united states";
            		Integer count = map.get(temp);

            		map.put(temp, (count == null) ? 1 : count + 1);
            	}
                Map<String, Integer> sortedMap = sortByComparator(map);
                
                
                System.out.println("");
                System.out.println("========= Countries statistic: ===========");
                System.out.println("== Number of Token Country: " + (int)country + " ==");
                printMap(sortedMap);

            	/*Set<String> uniqueSet = new HashSet<String>(list);
            	for (String temp : uniqueSet) {
            		System.out.println(temp + ": " + Collections.frequency(list, temp));
            	}*/
                
                
            }
            
            
        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static Map sortByComparator(Map unsortMap) {
    	 
		List list = new LinkedList(unsortMap.entrySet());
 
		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
			}
		});
 
		// put sorted list into map again
                //LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = ((LinkedList) list).descendingIterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
    
    public static void printMap(Map<String, Integer> map){
    	//for (int i = map.size(); i > 0; i--) {
			
    	for (Map.Entry<String, Integer> entry : map.entrySet()) {
    		System.out.println( entry.getKey() + "  "
    			+ entry.getValue());
    	}
     
      }

    private void initialiseGate() {
        
        try {
            // set GATE home folder
            // Eg. /Applications/GATE_Developer_7.0
            File gateHomeFile = new File("C:\\Program Files\\GATE_Developer_7.1");
            Gate.setGateHome(gateHomeFile);
            
            // set GATE plugins folder
            // Eg. /Applications/GATE_Developer_7.0/plugins            
            File pluginsHome = new File("C:\\Program Files\\GATE_Developer_7.1\\plugins");
            Gate.setPluginsHome(pluginsHome);            
            
            // set user config file (optional)
            // Eg. /Applications/GATE_Developer_7.0/user.xml
            //Gate.setUserConfigFile(new File("/Applications/GATE_Developer_7.0", "user.xml"));            
            
            // initialise the GATE library
            Gate.init();
            
            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);
            
            // flag that GATE was successfuly initialised
            isGateInitilised = true;
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}