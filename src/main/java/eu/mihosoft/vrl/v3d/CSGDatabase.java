package eu.mihosoft.vrl.v3d;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class CSGDatabase {
	
	private static HashMap<String,String> database=null;
	private static File db=new File("/.CSGdatabase.json");
    private static final Type TT_mapStringString = new TypeToken<HashMap<String,String>>(){}.getType();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static final HashMap<String,ArrayList<IParameterChanged>> parameterListeners=new HashMap<>();
	public static void set(String key, String value){
		synchronized(database){
			getDatabase().put(key, value);
		}
	}
	public static String get(String key){
		String ret =null;
		getDatabase();// load database before synchronization
		synchronized(database){
			ret=getDatabase().get(key);
		}
		return ret;
	}
	public static void setMM(String key, double value){
		setMicrons(key, (long) (value*1000.0));
	}
	public static void setMicrons(String key, long value){
		long oldVal = getMicrons( key);
		boolean newVal=oldVal!=value;
		
		set(key, new Long(value).toString());
		if(newVal){
			ArrayList<IParameterChanged> paraListeners = parameterListeners.get(key);
			if(paraListeners!=null){
				Parameter p = new Parameter(key) {
				};
				for(IParameterChanged params:paraListeners){
					params.parameterChanged(key, p);
				}
			}
		}
	}
	
	public void clearDatabase(){
		getDatabase();
		synchronized(database){
			database.clear();
		}
		parameterListeners.clear();
	}
	public void addParameterListener(String key, IParameterChanged l){
		if(parameterListeners.get(key)==null){
			parameterListeners.put(key, new ArrayList<>());
		}
		ArrayList<IParameterChanged> list = parameterListeners.get(key);
		if(!list.contains(l)){
			list.add(l);
		}
	}
	public void removeParameterListener(String key, IParameterChanged l){
		if(parameterListeners.get(key)==null){
			return;
		}
		ArrayList<IParameterChanged> list = parameterListeners.get(key);
		if(list.contains(l)){
			list.remove(l);
		}
	}
	
	public static double getMM(String key){
		return ((double)getMicrons( key))/1000.0;
	}
	public static long getMicrons(String key){
		getDatabase();// load database before synchronization
		String ret =get( key);
		return Long.parseLong(ret);
	}
	public static void delete(String key){
		synchronized(database){
			getDatabase().remove(key);
		}
	}
	private static HashMap<String,String> getDatabase() {
		if(database==null){
			new Thread(){
				public void run(){
					String jsonString;
					try {
						
						if(!db.exists()){
							setDatabase(new HashMap<String,String>());
						}
						else{
					        InputStream in = null;
					        try {
					            in = FileUtils.openInputStream(db);
					            jsonString= IOUtils.toString(in);
					        } finally {
					            IOUtils.closeQuietly(in);
					        }
					        HashMap<String,String> tm=gson.fromJson(jsonString, TT_mapStringString);
					        
					        
					        if(tm!=null){
//					        	System.out.println("Hash Map loaded from "+jsonString);
//					        	for(String k:tm.keySet()){
//						        	System.out.println("Key: "+k+" vlaue= "+tm.get(k));
//						        }
					        	setDatabase(tm);
					        }
						}
					} catch (Exception e) {
						e.printStackTrace();
						setDatabase(new HashMap<String,String>());
					}
					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run() {
							saveDatabase();
						}
					});
				}
			}.start();
			long start = System.currentTimeMillis();
			while(database==null){
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if((System.currentTimeMillis()-start)>500){
					setDatabase(new HashMap<String,String>());
				}
			}
		}
		return database;
	}
	
	public static void loadDatabaseFromFile(File f){
		InputStream in = null;
		String jsonString;
        try {
            try {
				in = FileUtils.openInputStream(db);
				jsonString= IOUtils.toString(in);
		        HashMap<String,String> tm=gson.fromJson(jsonString, TT_mapStringString);
		        for(String k:tm.keySet()){
		        	set(k,tm.get(k));
		        }
		        saveDatabase();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        } finally {
            IOUtils.closeQuietly(in);
        }
	}
	
	public static String getDataBaseString(){
		String writeOut=null;
		synchronized(database){
			 writeOut  =gson.toJson(database, TT_mapStringString); 
		}
		return writeOut;
	}
	
	public static void saveDatabase(){
		String writeOut=getDataBaseString();
		try {
			if(!db.exists()){
				db.createNewFile();
			}
	        OutputStream out = null;
	        try {
	            out = FileUtils.openOutputStream(db, false);
	            IOUtils.write(writeOut, out);
	            out.flush();
	            out.close(); // don't swallow close Exception if copy completes normally
	        } finally {
	            IOUtils.closeQuietly(out);
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void setDatabase(HashMap<String,String> database) {
		if(CSGDatabase.database!=null){
			return;
		}
		CSGDatabase.database = database;
	}
	public static void setMilidegrees(String name, long parameterInMilidegrees) {
		setMicrons(name, parameterInMilidegrees);
	}
	public static void setFixedPointScaledValue(String name, long parameterInFixedPointScaledValue) {
		setMicrons(name, parameterInFixedPointScaledValue);
	}
}