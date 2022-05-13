package fr.insee.arc.utils.structure;

import java.util.ArrayList;
import java.util.HashMap;

import fr.insee.arc.utils.exception.ArcException;

// TODO add documentation of the purpose of this class
public class GenericBean {

	public ArrayList<String> headers;
	public ArrayList<String> types;
	public ArrayList<ArrayList<String>> content;

	
	
	/**
	 * @param headers
	 * @param types
	 * @param content
	 */
	public GenericBean(ArrayList<String> headers, ArrayList<String> types,
			ArrayList<ArrayList<String>> content) {
		this.headers = headers;
		this.types = types;
		this.content = content;
	}

	/**
	 * @param requestResult
	 */
	public GenericBean(ArrayList<ArrayList<String>> requestResult) {
		// refactor de la méthode; faut pas utiliser la commande "remove" sinon on détruit le requestResult initial
		this.headers = requestResult.get(0);
		this.types = requestResult.get(1);
		this.content =new ArrayList<ArrayList<String>>();
		for (int i=2;i<requestResult.size();i++)
		{
			this.content.add(requestResult.get(i));
			
		}
	}

	/**
	 * Transform the headers ArrayList to a HashMap containing the index
	 * of headers (key: header, value: index)
	 *
	 * @return the resulting HashMap
	 */
	public HashMap<String, Integer> mapIndex() {
		HashMap<String, Integer> r = new HashMap<String, Integer>();
		for (int i = 0; i < headers.size(); i++) {
			r.put(this.headers.get(i), i);
		}
		return r;
	}

	/**
	 * Produce a HashMap containing headers and their relative types (header, type)
	 *
	 * @return the HashMap
	 */
	public HashMap<String, String> mapTypes() {
		HashMap<String, String> r = new HashMap<String, String>();

		for (int i = 0; i < headers.size(); i++) {
			r.put(this.headers.get(i), this.types.get(i));
		}
		return r;
	}

	/**
	 * Produce a HashMap containing headers and their relative content (header, content)
	 * return empty map if no records
	 *
	 * @return the HashMap
	 */
	public HashMap<String, ArrayList<String>> mapContent() {
		if (this.content == null || this.content.isEmpty()) {
			return new HashMap<>();
		}
		
		HashMap<String, ArrayList<String>> r = new HashMap<String, ArrayList<String>>();
		for (int i = 0; i < this.content.size(); i++) {

			for (int j = 0; j < this.content.get(i).size(); j++) {
				if (r.get(this.headers.get(j)) == null) {
					r.put(this.headers.get(j), new ArrayList<String>());
				}
				r.get(this.headers.get(j)).add(this.content.get(i).get(j));
			}
		}
		return r;
	}
	

    /**
     * Produce a HashMap containing headers and their relative content (header, content)
     * If content is empty, initialize the map with the column entries and empty list
     * @return
     */
    public HashMap<String, ArrayList<String>> mapContent(boolean initializeMapWithColumns)
    {
    	HashMap<String, ArrayList<String>> m = this.mapContent();
    	
		if (!initializeMapWithColumns) {
			return m;
		}
    	
    	// if headers found but no record returned from query, add column entry with empty list
    	if (!this.headers.isEmpty() && m.get(this.headers.get(0))==null)
    	{
    		for (int i=0; i<this.headers.size(); i++)
    		{
    			m.put(this.headers.get(i), new ArrayList<>());
    		}
    	}
    	return m;
    }
    
	
	public HashMap<String, Record> mapRecord() {
		if (this.content == null || this.content.size() == 0) {
			return new HashMap<String, Record>();
		}
		
		HashMap<String, Record> r = new HashMap<String, Record>();
		
		for (int i = 0; i < this.content.size(); i++) {

			for (int j = 0; j < this.content.get(i).size(); j++) {
				if (r.get(this.headers.get(j)) == null) {
						r.put(this.headers.get(j), new Record(this.types.get(j), new ArrayList<String>()));
				}
				r.get(this.headers.get(j)).data.add(this.content.get(i).get(j));
			}
		}
		return r;
	}
	

	public boolean isEmpty() {
		return size() == 0;
	}
	
    public int size() {
        return this.content.size();
    }
    
    public ArrayList<String> getHeadersUpperCase()
    {
    	ArrayList<String> l= new ArrayList<String>();
    	if (this.headers!=null)
    	{
	    	for (String s:this.headers)
	    	{
	    		l.add(s.toUpperCase());
	    	}
    	}
    	return l;
    }

	public HashMap<String, String> keyValue() throws ArcException {
		HashMap<String, String> r = new HashMap<>();
		
		if (!this.content.isEmpty() && this.content.get(0).size()!=2)
		{
			throw new ArcException("GenericBean keyValue : the set hasn't exactly 2 elements and cannot be mapped to key>value format");
		}
		
		for (ArrayList<String> line:this.content)
		{
			if (r.get(line.get(0))!=null)
			{
				throw new ArcException("GenericBean keyValue : duplicate key "+line.get(0)+" ");
			}
			r.put(line.get(0), line.get(1));
		}
		return r;
	}

	
	
	
}
