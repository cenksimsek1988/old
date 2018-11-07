package com.softactive.taxreturn.manager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.softactive.core.manager.ExcelWriter;
import com.softactive.core.manager.MyException;
import com.softactive.core.object.ExcelEntry;
import com.softactive.service.CriteriumService;
import com.softactive.service.IndicatorParentNameStandardService;
import com.softactive.taxreturn.object.IndicatorParentStandard;
import com.softactive.taxreturn.object.TaxReturnEntry;

@Component @Lazy
public class TaxReturnHandler extends AbstractTaxHandler<TaxReturnEntry>{
	private static final long serialVersionUID = 705861168932568047L;
	@Autowired
	private IndicatorParentNameStandardService ipss;
	private ExcelWriter writer;
	@Autowired
	private TaxReturnCriteriumHandler crHandler;
	@Autowired
	private CriteriumService cs;
	public static final String FILENAME = "kodVer";
	public static final String TAX_RETURN = "beyanname";
	public static final String SIGN = "sign";

	// tags to filter nodes to match from scheme
	public static final String CODE = "kod";
	public static final String TYPE = "tip";
	public static final String KIND = "turu";
	public static final String GENRE = "tur";
	public static final String FIRM_KIND = "isyeriTuru";
	public static final String ELSE = "diğer";
	public static final String[] TAGS_FOR_SCHEME = new String[] {
			CODE,
			TYPE,
			KIND,
			GENRE,
			FIRM_KIND,
			ELSE
	};
	
	public static final String DESCRIPTION = "aciklama";
	public static final String NAME = "ad";

	public static final String[] NAME_TAGS_FROM_SCHEME = new String[] {
			DESCRIPTION,
			NAME
	};


	public static final String PERIOD_CURRENT = "cd";
	public static final String PERIOD_PAST = "od";

	// regexes to search in tags to understand if the value in the node
	// precedent period.
	private static final String[] PAST_VALUE_REGEXES = new String[] {
			"oncekiDonem(.*)",
			PERIOD_PAST
	};

	private static final String[] CURRENT_VALUE_REGEXES = new String[] {
			"cariDonem(.*)",
			PERIOD_CURRENT
	};

	public static final String REGEX_SUFFIX = "(.*)";

	public static final String PERIOD = "donem";
	public static final String FREQUENCY = "tip";
	public static final String YEAR = "yil";


	private static final String NO_DATE = "no_date";
	private static final String MULTI_PERIOD = "multi_period";
	private static final String MULTI_INDICATOR = "multi_indicator";

	// Tags for single value node to prevent crossing codes with indicator name
	// If we notice such tags in a node, we will not change the unique code of indicator
	// if not means such node has just one code but multiple indicators lay inside in it
	// so we generate new unique codes from original code as base and the name of node in which
	// the value is written as suffix
	private static final String[] SINGLE_VALUE_TAGS = new String[] {
			"tutari",
			"tutar",
			"zarar",
			"isyeriSayisi"
	};

	private static final String[] INDEXED_TITLE_REGEXES = new String[] {
			"(.*)BKK",
			"(.*)Bolge",
			"(.*)Turu",
			"ilIlce",
			"ulke"
	};

//	private static final String[] FILTERED_PARENT_NAMES = new String[] {
//			"idari"
//	};

	private static final String[] COMMON_SCHEME_NODE_NAMES = new String[] {
			"ulke",
			"ilIlce"
	};

	private static final String[] TAGS_FOR_CONCAT = new String[] {
			"isletmeTuru"
	};

	private static final String[] SUFFIX_TO_RETAIN = new String[] {
			"BKK",
			"Bolge"
	};

	public static final String COMMON_CODES_SCHEME_PATH = "src/main/resources/version/Kodlar.xml";
	
//	private LocalDate lastDate;
//	private String frq;
	
	public TaxReturnHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
		writer = new ExcelWriter();
	}

	@Override
	protected List<Element> getArray(Document r) {
		return resolveAllNodes(r);
	}
	
	@Override
	protected void mapMetaData(Document r) throws MyException {
		Element main = (Element) r.getElementsByTagName(TAX_RETURN).item(0);

		//match code scheme xml with uploaded tax return xml document
		String name = main.getAttribute(FILENAME);
		
		TreeMap<String, String> taxReturnInfo = new TreeMap<>();
		taxReturnInfo.put(ExcelEntry.CODE, "Beyanname Türü");

		taxReturnInfo.put("İsim", name.split("_")[0]);
		writer.write(taxReturnInfo, "Beyanname");
		
		String filePath = "src/main/resources/version/" + name + "_Kodlar.xml";
		Document codeScheme = getCodeScheme(filePath);
		if(codeScheme==null) {
			throw new MyException("cannot find any attribute in meta data with name: " + FILENAME +
					"\nmeta data element: " + main);
		} else {
			sharedParams.put(PARAM_VERSIONED_SCHEME_XML, codeScheme);
		}

		//period
		Element period = (Element) r.getElementsByTagName(PERIOD).item(0);

		//frequency
		String frq = getValue(period, FREQUENCY);
		sharedParams.put(PARAM_FREQUENCY_ID, frq);

		//date
		String yearString = getValue(period, YEAR);
		int y = resolveValidInteger(yearString);

		LocalDate lastDate = new LocalDate(y, 12, 31);
		sharedParams.put(PARAM_DATE, lastDate);
		
		Document commonsScheme = getCodeScheme(COMMON_CODES_SCHEME_PATH);
		sharedParams.put(PARAM_COMMONS_SCHEME_XML, commonsScheme);
		
		sharedParams.put(PARAM_WORKBOOK, writer.getWorkbook());
		
		writer.stampLastDate(lastDate);
	}

	@Override
	public void onListSuccesfullyParsed(List<TaxReturnEntry> list) {
		System.out.println("one xml file is parsed and added into excel. " + list.size() + " entries are resolved");
	}
	
	

	public void checkCriteria() {
		sharedParams.put(PARAM_EXCEL_WRITER, writer);
		crHandler.handle(cs);
	}

	private Document getCodeScheme(String filePath) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(filePath);
			doc.normalize();
			return doc;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int firstCapitalIndex(String str) {        
		for(int i = 1; i < str.length(); i++) {
			if(Character.isUpperCase(str.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private Element getParentNode(String parentName, Document codeScheme) {
		Element parentNode = (Element) codeScheme.getElementsByTagName(parentName).item(0);
		if(parentNode!=null) {
			return parentNode;
		}
		List<IndicatorParentStandard> list = ipss.findAlternativesByName(parentName);
		if(list!=null) {
			for(IndicatorParentStandard ips:list) {
				parentNode = (Element) codeScheme.getElementsByTagName(ips.getName()).item(0);
				if(parentNode!=null) {
					return parentNode;
				}
			}
		}
		return null;
	}

	private TaxReturnEntry getBaseEntry(String name, String parentName, String code, Map<String, Object> sharedParams)  throws MyException{
		Document codeScheme = null;
		Element parentNode = null;
		boolean commonScheme = false;
		for(String filter:COMMON_SCHEME_NODE_NAMES) {
			if(name.equals(filter)) {
				codeScheme = (Document) sharedParams.get(PARAM_COMMONS_SCHEME_XML);
				commonScheme = true;
			}
		}
		if(codeScheme==null) {
			codeScheme = (Document) sharedParams.get(PARAM_VERSIONED_SCHEME_XML);
		}
		if(commonScheme) {
			NodeList tempList = codeScheme.getElementsByTagName(name);
			Node tempNode = null;
			if(tempList != null && tempList.getLength()>0){
				tempNode = tempList.item(0);
			} else {
				List<IndicatorParentStandard> list = ipss.findAlternativesByName(name);
				if(list!=null) {
					for(IndicatorParentStandard ips:list) {
						parentNode = (Element) codeScheme.getElementsByTagName(ips.getName()).item(0);
						if(parentNode!=null) {
							break;
						}
					}
				}
			}
			if(tempNode != null) {
				parentNode = (Element) tempNode.getParentNode();
			}
		}
		if(parentNode == null) {
			parentNode = getParentNode(parentName, codeScheme);
		}
		if(parentNode == null) {
			for(String suffix:SUFFIX_TO_RETAIN) {
				if(name.matches(REGEX_SUFFIX + suffix)) {
					String tmpParentName = suffix.toLowerCase();
					parentNode = (Element) getParentNode(tmpParentName, codeScheme).getParentNode();
					break;
				}
			}
			for(String tag:TAGS_FOR_CONCAT) {
				String tmpParentName = parentName;
				if(name.equals(tag)) {
					if(parentName.matches("yurtDisi(.*)")) {
						tmpParentName = tmpParentName.substring("yurtdisi".length());
					}
					int firstCap = firstCapitalIndex(tmpParentName);
					if(firstCap != -1) {
						tmpParentName = tmpParentName.substring(0, firstCap);
						String firstChar = String.valueOf(tmpParentName.charAt(0)).toLowerCase();
						tmpParentName = firstChar + tmpParentName.substring(1);
					}
					String firstChar = String.valueOf(tag.charAt(0)).toUpperCase();
					tag = firstChar + tag.substring(1);
					tmpParentName = tmpParentName + tag;
					// generated parent name will find a child node
					// but we need the parent to iterate over.
					Node tmpNode =  getParentNode(tmpParentName, codeScheme);
					if(tmpNode!=null) {
						parentNode = (Element) tmpNode.getParentNode();
					}
					break;
				}

			}
		}
		if(parentNode == null) {
			TaxReturnEntry ent = getWhiteEntry(name);
			ent.setParentName(parentName);
			LocalDate current = (LocalDate) sharedParams.get(PARAM_DATE);
			ent.addPrice(current.toString(), code);
			return ent;			
		}
		NodeList list = parentNode.getChildNodes();
		String[] codeTitles = new String[] {
				CODE,
				name
		};
		for(String title:codeTitles) {
			for(int i = 0; i < list.getLength(); i++) {
				Node n = list.item(i);
				if(n instanceof Element) {
					Element e = (Element) list.item(i);
					Element eCode = (Element) e.getElementsByTagName(title).item(0);
					if(eCode == null) {
						break;
					}
					if(eCode.getTextContent().equals(code)) {
						return getBaseEntry(e, name, code);
					}
				}
			}
		}
		throw new MyException("no base entry object is created.\nparent name: " + parentName + "\nname: " + name +
				"\ncode: " + code + "\nparent node from scheme\\nparent name: " +  parentNode.getParentNode().getNodeName() +
				"\nname: " + parentNode.getNodeName());
	}

	private TaxReturnEntry getBaseEntry(Element e, String parentName, String code) throws MyException {
		TaxReturnEntry entry = new TaxReturnEntry();
		entry.setCode(code);
		TreeMap<String, String> values = getAsMap(e);
		String name = null;
		for(String tag:NAME_TAGS_FROM_SCHEME) {
			name = values.get(tag);
			if(name != null) {
				break;
			}
		}
		if(name!=null) {
			entry.setName(name);
			entry.setParentName(parentName);
			return entry;
		}
		throw new MyException("couldn't detect name tag from scheme node. The value map:\n" + values);
	}

	@Override
	protected String[] getArrayTags() {
		return TAGS_FOR_SCHEME;
	}

	private LocalDate getPeriodBefore(LocalDate current, String frq) throws MyException {
		if(current == null) {
			throw new MyException("current date object is null");
		}
		LocalDate past = current.minusYears(1);
		return past;
	}

	private String getDate(Node n) throws MyException {
		String dateString = n.getTextContent();
		LocalDate date = resolveValidDate(dateString + "-12-31");
		return date.toString();
	}

	private void write(Node parent) throws MyException{
		NodeList list = parent.getChildNodes();
		TreeMap<String, TaxReturnEntry> entries = new TreeMap<String, TaxReturnEntry>();
		TreeMap<String, String> extras = new TreeMap<>();
		String singleDateFromChild = NO_DATE;
		TaxReturnEntry baseEntry = null;
		String parentName = parent.getNodeName();
		if(parentName.length()>31) {
			parentName = parentName.substring(0, 31);
		}
		for(int i = 0; i < list.getLength(); i++) {
			try {
				Node child = list.item(i);
				String name = child.getNodeName();
				if(name.startsWith("#")) {
					continue;
				}
				if(child.getChildNodes().getLength()>1) {
					continue;
				}
				String value = child.getTextContent();
				boolean keyNode = false;
				for(String key:TAGS_FOR_SCHEME) {
					if(name.equals(key)) {
						parentName = parent.getParentNode().getNodeName();
						try {
							baseEntry = getBaseEntry(name, parentName, value, sharedParams);
							if(name == CODE) {
								baseEntry.setCode(null);
							}
						} catch (MyException e) {
							System.out.println(e);
						}
						if(parentName.length()>31) {
							parentName = parentName.substring(0, 31);
						}
						keyNode = true;
						break;
					}
				}
				if(keyNode) {
					continue;
				}
				if(name.equals(YEAR)) {
					singleDateFromChild = getDate(child);
					for(String entryKey: entries.keySet()) {
						TaxReturnEntry existingEntry = entries.get(entryKey);
						for(String priceKey: existingEntry.getPriceKeys()) {
							String existingValue = existingEntry.getPrice(priceKey);
							existingEntry.addPrice(singleDateFromChild, existingValue);
							existingEntry.removePrice(priceKey);
						}
					}
					continue;
				}
				boolean indexedIndicatorNode = false;
				for(int t = 1; t < 3; t++) {
					if(name.matches(REGEX_SUFFIX + t)) {
						TaxReturnEntry newEntry = entries.get(String.valueOf(t));
						if(newEntry == null) {
							newEntry = getWhiteEntry(parentName);
						}
						for(String regex:PAST_VALUE_REGEXES) {
							if(name.matches(regex)) {
								String entryName = name.substring(regex.length());
								newEntry.setName(entryName);
								newEntry.setCode(entryName);
								String frq = (String) sharedParams.get(PARAM_FREQUENCY_ID);
								LocalDate current = (LocalDate) sharedParams.get(PARAM_DATE); 
								LocalDate past = null;
								past = getPeriodBefore(current, frq);
								String pricePast = null;
								pricePast = resolveValidString(value, null);
								newEntry.addPrice(past.toString(), pricePast);
								entries.put(String.valueOf(t), newEntry);
								indexedIndicatorNode = true;
								break;
							}
						}
						if(indexedIndicatorNode) {
							break;
						}
						for(String regex:CURRENT_VALUE_REGEXES) {
							if(name.matches(regex)) {
								String entryName = name.substring(regex.length());
								newEntry.setName(entryName);
								newEntry.setCode(entryName);
								LocalDate current = (LocalDate) sharedParams.get(PARAM_DATE);
								newEntry.addPrice(current.toString(), resolveValidString(value, null));
								entries.put(String.valueOf(t), newEntry);
								indexedIndicatorNode = true;
								break;
							}
						}
					}
				}
				if(indexedIndicatorNode) {
					continue;
				}
				boolean pastValueNode = false;
				for(String regex:PAST_VALUE_REGEXES) {
					if(name.matches(regex)) {
						TaxReturnEntry newEntry = entries.get(MULTI_PERIOD);
						if(newEntry == null) {
							newEntry = getWhiteEntry(parentName);
						}
						String frq = (String) sharedParams.get(PARAM_FREQUENCY_ID);
						LocalDate current = (LocalDate) sharedParams.get(PARAM_DATE); 
						LocalDate past = null;
						past = getPeriodBefore(current, frq);
						String pricePast = resolveValidString(value, null);
						newEntry.addPrice(past.toString(), pricePast);
						entries.put(MULTI_PERIOD, newEntry);
						pastValueNode = true;
						break;
					}
				}
				if(pastValueNode) {
					continue;
				}
				boolean currentValueNode = false;
				for(String regex:CURRENT_VALUE_REGEXES) {
					if(name.matches(regex)) {
						TaxReturnEntry newEntry = entries.get(MULTI_PERIOD);
						if(newEntry == null) {
							newEntry = getWhiteEntry(parentName);
						}
						LocalDate current = (LocalDate) sharedParams.get(PARAM_DATE);
						newEntry.addPrice(current.toString(), resolveValidString(value, null));
						entries.put(MULTI_PERIOD, newEntry);
						currentValueNode = true;
						break;
					}
				}
				if(currentValueNode) {
					continue;
				}
				boolean indexedTitleNode = false;
				for(String regex: INDEXED_TITLE_REGEXES) {
					if(name.matches(regex)) {
						TaxReturnEntry tempEntry = null;
						try {
							tempEntry = getBaseEntry(name, parentName, value, sharedParams);
						} catch (MyException e) {
							System.out.println(e);
							continue;
						}
						extras.put(name, tempEntry.getName());
						indexedTitleNode = true;
						break;
					}
				}
				if(indexedTitleNode) {
					continue;
				}
				String price = resolveValidString(value, null);
				TaxReturnEntry newEntry = getWhiteEntry(parentName);
				newEntry.setCode(name);
				newEntry.setName(name);
				newEntry.addPrice(singleDateFromChild, price);
				entries.put(MULTI_INDICATOR + name, newEntry);
				continue;
			} catch (MyException e) {
				continue;
			}
		}
		adjustAndWrite(entries, extras, baseEntry, parentName, singleDateFromChild, sharedParams);
	}

	private TaxReturnEntry getWhiteEntry(String parentName) {
		TaxReturnEntry whiteEntry = new TaxReturnEntry();
		whiteEntry.setCode(parentName);
		whiteEntry.setName(parentName);
		whiteEntry.setParentName(parentName);
		return whiteEntry;
	}

	private void adjustAndWrite(TreeMap<String, TaxReturnEntry> entries, TreeMap<String, String> extras, TaxReturnEntry baseEntry, String parentName, String singleDateFromChild, Map<String, Object> sharedParams) throws MyException{
		if(entries.size()==0) {
			entries.put(parentName, getWhiteEntry(parentName));
		}
		for(String entryKey:entries.keySet()) {
			TaxReturnEntry en = entries.get(entryKey);
			if(entryKey.equals(MULTI_PERIOD)) {
				if(baseEntry!=null) {
					en.setCode(baseEntry.getCode());
					en.setName(baseEntry.getName());
				}
			}
			if(entryKey.startsWith(MULTI_INDICATOR)) {
				if(baseEntry!=null) {
					en.setCode(baseEntry.getCode() + "_" + en.getCode());
					en.setName(baseEntry.getName());
				} else {
					String name = entryKey.substring(MULTI_INDICATOR.length());
					en.setCode(name);
					en.setName(name);
				}
			}
			for(String priceKey:en.getPriceKeys()) {
				if(priceKey == NO_DATE) {
					String existingValue = en.getPrice(NO_DATE);
					en.removePrice(NO_DATE);
					String dateString = null;
					if(!singleDateFromChild.equals(NO_DATE)) {
						dateString = singleDateFromChild;
					} else {
						dateString = getCurrentDateString(sharedParams);
					}
					en.addPrice(dateString, existingValue);
				}
				en.setExtras(extras);
			}
			try {
				writer.write(en.getValuesAsMap(), parentName);
			} catch (MyException e) {
				System.out.println(e);
			}
		}
	}

	private String getCurrentDateString(Map<String, Object> sharedParams) {
		LocalDate current = (LocalDate) sharedParams.get(PARAM_DATE);
		return current.toString();
	}

	@Override
	protected TaxReturnEntry getObject(Element o) throws MyException {
		TaxReturnEntry baseEntry = null;
		write(o);
		return baseEntry;
	}

	@Override
	protected boolean isOutputInvalid(List<TaxReturnEntry> output) {
		// TODO Auto-generated method stub
		return false;
	}
}
