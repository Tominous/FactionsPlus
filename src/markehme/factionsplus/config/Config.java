package markehme.factionsplus.config;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.Map.Entry;

import markehme.factionsplus.*;
import java.util.Map.Entry;
import markehme.factionsplus.*;
import markehme.factionsplus.FactionsBridge.*;
import markehme.factionsplus.config.sections.*;
import markehme.factionsplus.config.yaml.*;
import markehme.factionsplus.extras.*;
import markehme.factionsplus.util.*;

import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import com.avaje.ebean.enhance.agent.*;



public abstract class Config {// not named Conf so to avoid conflicts with com.massivecraft.factions.Conf

	// could use Plugin.getDataFolder() (tho no need) and move these to onEnable() or onLoad() else will likely NPE if using
	// getDataFolder()
	// just // never // be // "" // cause // that // means // root // folder
	public static final File				folderBase				= new File( "plugins" + File.separator + "FactionsPlus" );
	public static final File				folderWarps				= new File( folderBase, "warps" );
	public static final File				folderJails				= new File( folderBase, "jails" );
	public static final File				folderAnnouncements		= new File( folderBase, "announcements" );
	public static final File				folderFRules			= new File( folderBase, "frules" );
	public static final File				folderFBans				= new File( folderBase, "fbans" );
	public static final File				fileDisableInWarzone	= new File( folderBase, "disabled_in_warzone.txt" );
	
	public static File						templatesFile			= new File( folderBase, "templates.yml" );
	public static FileConfiguration			templates;
	
	// this // file // is // located // inside // .jar // in // root // dir
	private static final String				fileConfigDefaults		= "config_defaults.yml";
	// and it contains the defaults, so that they are no longer hardcoded in java code
	private static File						fileConfig				= new File( Config.folderBase, "config.yml" );
	
	// never change this, it's yaml compatible:
	public static final char				DOT						= '.';
	
	// Begin Config Pointers
	
	/**
	 * Caveats: YOU CAN rename all the fields, it won't affect config.yml because it uses the name inside the annotation above
	 * it
	 * if you rename the realAlias inside the annotation then you'll have to be adding oldaliases to each of their fields
	 * (children) and to their
	 * child @ConfigSection 's fields and so on;
	 * cantfix: adding old aliases for (sub)sections should not be doable, because I cannot decide which parent's oldAlias would
	 * apply to the child's alias when computing the dotted format of the child
	 * 
	 * you may change order of these fields (or section's fields) but this won't have any effect if config.yml already existed,
	 * only if new one is about to be created<br>
	 * <br>
	 * fields could be named _something so that when you type Config._ and code completion with Ctrl+Space you can see only the
	 * relevant fields<br>
	 */
	@Section(
			realAlias_neverDotted = "jails" )
	public static final Section_Jails		_jails					= new Section_Jails();
	
	
	@Section(
			realAlias_neverDotted = "warps" )
	public static final Section_Warps		_warps					= new Section_Warps();
	
	// cantfix: if you rename the section, you've to add oldaliases for each leaf found in the tree of it, avoid this by
	// allowing
	// oldaliases for section
	@Section(
			realAlias_neverDotted = "banning" )
	public static final Section_Banning		_banning				= new Section_Banning();
	
	
	@Section(
			realAlias_neverDotted = "rules" )
	public static final Section_Rules		_rules					= new Section_Rules();
	
	@Section(
			realAlias_neverDotted = "peaceful" )
	public static final Section_Peaceful	_peaceful				= new Section_Peaceful();
	
	@Section(
			realAlias_neverDotted = "powerboosts" )
	public static final Section_PowerBoosts	_powerboosts			= new Section_PowerBoosts();
	
	@Section(
			realAlias_neverDotted = "announce" )
	public static final Section_Announce	_announce				= new Section_Announce();
	
	
	@Section(
			realAlias_neverDotted = "economy" )
	public static final Section_Economy		_economy				= new Section_Economy();
	
	
	@Section(
			comments = {
		"some comment here, if any", "second line of comment"
			}, realAlias_neverDotted = "Teleports" )
	public static final Section_Teleports	_teleports				= new Section_Teleports();
	
	@Section(
			realAlias_neverDotted = "extras" )
	public final static Section_Extras		_extras					= new Section_Extras();
	
	@Option(
			realAlias_inNonDottedFormat = "DoNotChangeMe" )
	// this is now useless, FIXME: remove this field, OR rename and increment it every time something changes in the config ie.
	// coder adds new options or removes or changes/renames config options but not when just changes their values (id: value)
	public static final _int				_doNotChangeMe			= new _int( 11 );
	
	// the root class that contains the @ConfigSection and @ConfigOptions to scan for
	private static final Class				configClass				= Config.class;
	// End Config
	
	
	
	private static File						currentFolder_OnPluginClassInit;
	private static File						currentFolder_OnEnable	= null;
	private static boolean					inited					= false;
	
	
	/**
	 * call this one time onEnable() never on onLoad() due to its evilness;)<br>
	 */
	public final static void init() {
		boolean failed = false;
		// try {
		if ( Q.isInconsistencyFileBug() ) {
			throw FactionsPlusPlugin.bailOut( "Please do not have `user.dir` property set, it will mess up so many things"
				+ "(or did you use native functions to change current folder from the one that was on jvm startup?!)" );
		}
		
		if ( hasFileFieldsTrap() ) {
			throw FactionsPlusPlugin.bailOut( "there is a coding trap which will likely cause unexpected behaviour "
				+ "in places that use files, tell plugin author to fix" );
		}
		
		// first make sure the (hard)coded options are valid while at the same time build a list of all obsolete+new
		// option
		// names
		
		Typeo.sanitize_AndUpdateClassMapping( configClass );
		
		// map.key: dotted format config.yml settings(only key: value ones)
		// map.value: Field.class instance of the
		
		// Field f;
		
		// throw null;
		// Annotation[] ar = f.getDeclaredAnnotations();
		// Class.class.getDeclaredAnnotations();
		// } catch ( Throwable t ) {
		// failed = true;
		// Q.rethrow( t );
		// } finally {
		// if ( failed ) {
		// FactionsPlus.instance.setDisAllowPluginToEnable();//FIXME: this has no effect now
		// }
		// }
		
		
	}
	
	
	// public static final String getDottedFormat( Field f ) {
	// // TODO:
	// throw Q.ni();
	// }
	
	
	
	/**
	 * make sure all the File fields in this class that are likely used somewhere else in constructors like new File(field,
	 * myfile);
	 * are non-empty to avoid 'myfile' being in root of drive instead of just current folder as expected<br>
	 * this would cause some evil inconsistencies if any of those fields would resolve to empty paths<br>
	 */
	private static boolean hasFileFieldsTrap() {
		Class classToCheckFor_FileFields = Config.class;
		Field[] allFields = classToCheckFor_FileFields.getFields();
		for ( Field field : allFields ) {
			if ( File.class.equals( field.getType() ) ) {
				// got one File field to check
				try {
					File instance = (File)field.get( classToCheckFor_FileFields );
					if ( instance.getPath().isEmpty() ) {
						// oops, found one, to avoid traps where you expect new File( instance, yourfile);
						// to have 'yourfile' in root folder of that drive ie. '\yourfile' instead of what you might
						// expect "yourfile" to be just in current folder just like a new File(yourfile) would do
						return true;
					}
				} catch ( IllegalArgumentException e ) {
					Q.rethrow( e );
				} catch ( IllegalAccessException e ) {
					Q.rethrow( e );
				}
			}
		}
		return false;
	}
	
	
	/**
	 * called on plugin.onEnable() and every time you want the config to reload
	 */
	public final static void reload() {
		
		Config.setInited( false );// must be here to cause config to reload on every plugin(s) reload from console
		Config.templates = null;
		boolean failed = false;
		try {
			
			Config.ensureFoldersExist();
			
			reloadConfig();
			
			
			Config.templates = YamlConfiguration.loadConfiguration( Config.templatesFile );
			
			// _enableJails = ( (Boolean);
			// System.out.println(Config.config.getInt( str_economyCostToAnnounce) );
			
			// last:
			Config.setInited( true );
		} catch ( Throwable t ) {
			Q.rethrow( t );
		} finally {
			if ( failed ) {
				FactionsPlus.instance.disableSelf();// must make sure we're disabled if something failed if not /plugins would
													// show us green
				// but mostly, for consistency's sake and stuff we couldn't think of/anticipate now
			}
		}
	}
	
	
	private static void setInited( boolean nowState ) {
		inited = nowState;
	}
	
	
	protected static void ensureFoldersExist() {
		File dataF = FactionsPlus.instance.getDataFolder();
		if ( !dataF.equals( folderBase ) ) {
			throw FactionsPlusPlugin
				.bailOut( "Base folder and dataFolder differ, this may not be intended and it may just be a possible bug in the code;"
					+ "folderBase=" + folderBase + " dataFolder=" + dataF );
		}
		
		try {
			addDir( Config.folderBase );
			addDir( Config.folderWarps );
			addDir( Config.folderJails );
			addDir( Config.folderAnnouncements );
			addDir( Config.folderFRules );
			addDir( Config.folderFBans );
			
			if ( !Config.fileDisableInWarzone.exists() ) {
				Config.fileDisableInWarzone.createNewFile();
				FactionsPlusPlugin.info( "Created file: " + Config.fileDisableInWarzone );
			}
			
			if ( !Config.templatesFile.exists() ) {
				
				FactionsPlusTemplates.createTemplatesFile();
				FactionsPlusPlugin.info( "Created file: " + Config.templatesFile );
			}
			
		} catch ( Exception e ) {
			e.printStackTrace();
			throw FactionsPlusPlugin.bailOut( "something failed when ensuring the folders exist" );
		}
	}
	
	
	private static final void addDir( File dir ) {
		if ( !dir.exists() ) {
			if ( dir.getPath().isEmpty() ) {
				throw FactionsPlusPlugin.bailOut( "bad coding, this should usually not trigger here, but earlier" );
			}
			FactionsPlusPlugin.info( "Added directory: " + dir );
			dir.mkdirs();
		}
	}
	
	
	
	private final static String	bucketOfSpaces	= new String( new char[WannabeYaml.maxLevelSpaces] ).replace( '\0', ' ' );
	
	
	/**
	 * this works for yaml's .getValues(deep)
	 * 
	 * @param level
	 * @param start
	 * @throws IOException
	 */
	private final static void parseWrite( int level, Map<String, Object> start ) throws IOException {
		for ( Map.Entry<String, Object> entry : start.entrySet() ) {
			Object val = entry.getValue();
			String key = entry.getKey();
			if ( level > 0 ) {
				bw.write( bucketOfSpaces, 0, WannabeYaml.spacesPerLevel * level );
			}
			bw.write( key );
			bw.write( WannabeYaml.IDVALUE_SEPARATOR );
			if ( !( val instanceof MemorySection ) ) {
				bw.write( " " + val );
				bw.newLine();
			} else {
				bw.newLine();
				parseWrite( level + 1, ( (MemorySection)val ).getValues( false ) );
			}
		}
	}
	
	
	// private static final LinkedList<WYItem> llist = new LinkedList<WYItem>();
	
	
	private final static void appendSection( int level, WYSection root ) throws IOException {
		assert Q.nn( root );
		WYItem currentItem = root.getFirst();
		
		while ( null != currentItem ) {
			
			Class<? extends WYItem> cls = currentItem.getClass();
			// System.out.println(currentItem+"!");
			
			if ( level > 0 ) {
				bw.write( bucketOfSpaces, 0, WannabeYaml.spacesPerLevel * level );
			}
			
			if ( currentItem instanceof WYRawButLeveledLine ) {
				bw.write( ( (WYRawButLeveledLine)currentItem ).getRawButLeveledLine() );
				bw.newLine();
			} else {
				
				if ( !( currentItem instanceof WY_IDBased ) ) {
					throw FactionsPlus.bailOut( "impossible, coding bug detected" );
				}
				
				
				if ( WYIdentifier.class == cls ) {
					WYIdentifier wid = ( (WYIdentifier)currentItem );
					// System.out.println(wid.getInAbsoluteDottedForm(virtualRoot));
					bw.write( wid.getId() );
					bw.write( WannabeYaml.IDVALUE_SEPARATOR );
					bw.write( WannabeYaml.space + wid.getValue() );
					bw.newLine();
				} else {
					if ( WYSection.class == cls ) {
						WYSection cs = (WYSection)currentItem;
						bw.write( ( cs ).getId() + WannabeYaml.IDVALUE_SEPARATOR );
						bw.newLine();
						appendSection( level + 1, cs );// recurse
					} else {
						// throw null;//FIXME: throw right one
						throw FactionsPlus.bailOut( "impossible, coding bug detected" );
					}
				}
			}
			currentItem = currentItem.getNext();
		}
	}
	
	/**
	 * a mapping between the Field config option and an ordered list of dottedform of and WYIdentifiers encountered in
	 * config.yml<br>
	 * dotted form ie. extras.lwc.someid
	 */
	private static final HashMap<Field, TypedLinkedList<DualPack<String, WYIdentifier>>>	mapField_to_ListOfWYIdentifier	=
																																new HashMap<Field, TypedLinkedList<DualPack<String, WYIdentifier>>>();
	
	
	
	/**
	 * must be inside: synchronized ( mapField_to_ListOfWYIdentifier )<br>
	 * this parses the entire representation of the config.yml and marks duplicates and invalid configs<br> 
	 * @param root
	 */
	private final static void parseOneTime_and_CheckForValids( WYSection root, String dottedParentSection ) {
		assert Q.nn( root );
		WYItem<COMetadata> currentItem = root.getFirst();
		// WYSection parent = root;
		// int level=0;
		// while ( null != parent ) {
		boolean isTopLevelSection = ( null == dottedParentSection ) || dottedParentSection.isEmpty();
		
		while ( null != currentItem ) {
			
			Class<? extends WYItem> cls = currentItem.getClass();
			
			
			if ( WYSection.class == cls ) {
				WYSection cs = (WYSection)currentItem;
				// sections are not checked for having oldaliases mainly since they are part of the dotted form of a config
				// options and thus
				// are indirectly checked when config options(aka ids) are checked
				String dotted = ( isTopLevelSection ? cs.getId() : dottedParentSection + Config.DOT + cs.getId() );
				
				parseOneTime_and_CheckForValids( cs, dotted );// recurse
				// parent = cs;
				// currentItem = cs.getFirst();
			} else {
				if ( WYIdentifier.class == cls ) {
					WYIdentifier<COMetadata> wid = ( (WYIdentifier)currentItem );
					String dotted = ( isTopLevelSection ? wid.getId() : dottedParentSection + Config.DOT + wid.getId() );
					// String dotted = wid.getID_InAbsoluteDottedForm( virtualRoot );
					// System.out.println( dotted );
					Field foundAsField = Typeo.getField_correspondingTo_DottedFormat( dotted );
					if ( null == foundAsField ) {
						// done: invalid config option encountered in config.yml transforms into comment
						// WYSection widsParent = wid.getParent();
						// assert null ! it just wouldn't ever be null, else bad coding else where heh
						COMetadata oldmd = wid.setMetadata( new CO_Invalid( wid, dotted ) );
						assert null == oldmd : "should not already have metadata, else we failed somewhere else";
					} else {
						// System.out.println( "!!!" + dotted );
						// TODO: must check if config.yml has the same id twice or more, if yes then what? last overrides?
						// or throw
						// or move extras into file?
						
						// : we can let the HashMap check if one already exists even though they will != but they will
						// .equals()
						// if we define that
						// so if two differed(subsequent) dotted forms map to the same Field, then we found duplicate
						// options in
						// .yml file
						// well actually no, the above is false premising in the current context
						
						TypedLinkedList<DualPack<String, WYIdentifier>> existingWYIdList =
							mapField_to_ListOfWYIdentifier.get( foundAsField );
						if ( null == existingWYIdList ) {
							// first time creating the list for this Field 'found'
							// which also means there should be no duplicate checks in this {} block
							existingWYIdList = new TypedLinkedList<DualPack<String, WYIdentifier>>();
							TypedLinkedList<DualPack<String, WYIdentifier>> impossible =
								mapField_to_ListOfWYIdentifier.put( foundAsField, existingWYIdList );
							assert null == impossible : "this just cannot freaking happen, but still, can never really `know when you're missing something` aka `be sure`";
							assert existingWYIdList == mapField_to_ListOfWYIdentifier.get( foundAsField );
							
							
							existingWYIdList.addLast( new DualPack( dotted, wid ) );// add all config options one by one in
																					// the
																					// order of occurrence
							// in
							// config.yml
							
							assert existingWYIdList.contains( new DualPack( dotted, wid ) );
							
						} else {
							// check only if the list wasn't empty, if we're here it wasn't, thus it may already have at
							// least 1
							// element which we
							// must check against and see if wid isn't already existing there (as different instance though)
							// does id already exist, ie. duplicate encountered in .yml ?
							
							// FIXME: this compares wid regardless of parents, but we must compare their dotted form instead
							// so either store hashmap or make sure equals compares dotted forms
							// hashmap will be faster, a hashmap of dotted -> wid
							// or a wid.setEqualsComparesIncludingParentsUpTo(virtualRoot) - naah this one is too much
							// overhead,
							// hashmap ftw!
							
							
							int index = existingWYIdList.indexOf( new DualPack( dotted, WYIdentifier.NULL ) );
							// seeks dotted format 'wid' in list by doing .equals() on each of
							// them // inside // the list
							if ( index >= 0 ) {// exists already ?
								WYIdentifier activeCfgOption = existingWYIdList.get( index ).getSecond();
								int activeLine = activeCfgOption.getLineNumber();
								
								COMetadata oldmd = wid.setMetadata( new CO_Duplicate( wid, dotted, activeCfgOption ) );
								assert null == oldmd : "should not already have metadata, else we failed somewhere else";
								
								// WYSection widsParent = wid.getParent();
								// TODO: also check if it is in any other lists, it probably isn't at this time.
								// currentItem = widsParent.replaceAndTransformInto_WYComment( wid, commentPrefixForDUPs );
								// wid.replaceAndTransformSelfInto_WYComment();
								// so we still have a getNext() to go to, after wid is basically destroyed(at
								// least its getNext will be null after this)
								// let's not forget to remove this from list,
								// existingWYIdList.remove( index );// a MUST
								// assert existingWYIdList.contains( wid );
								// System.out.println(existingWYIdList.get( index ));
								// existingWYIdList.add( index, currentItem );
								// assert !existingWYIdList.contains( wid );
								// assert existingWYIdList.contains( currentItem);
								
								// this means, it will compare id without considering values (as per WYIdentifier's
								// .equals()
								
								// if we're here this will work:
								
								
								// TODO: what to do when same config is encountered twice, does it override the prev one? do
								// we
								// stop? do
								// we move it to some file for reviewal? or do we comment it out?
							} else {
								// doesn't exit, we add it to list
								existingWYIdList.addLast( new DualPack( dotted, wid ) );
							}
						}
						assert null != existingWYIdList;// obviously
						
						
					}// end of else found
					
					// Object rtcid = getRuntimeConfigIdFor( wid );// pinpoint an annotated field in {@Link Config.class}
					// if ( null == rtcid ) {
					// // there isn't a runtime option for the encountered id(=config option name)
					// // therefore we check if it's an old alias
					// Object newId = getNewIdForTheOldAlias( wid );// if any
					// if ( null != newId ) {
					// // not old alias either
					// // thus it's an invalid config option encountered
					// String failMsg = "invalid config option encountered: " + wid;// TODO: also show the in config
					// line/pos
					// // for it
					// // first make sure it won't be written on next config save, by removing it from chain
					// // wid.removeSelf();
					// throw new RuntimeException( failMsg );//it won't be written to config if we abort
					// }
					//
					// // we then found an old alias for this id, since we're here
					// if ( newId.encounteredAliasesCount() < 1 ) {
					// // update the newID's value with the old alias' value
					// newId.setValue( wid.getValue() );
					// newId.addEncounteredOldAlias( wid );
					// } else {
					// // we already encountered an alias for this id
					// // how would we know which is the right value
					// // for now we consider the last encountered alias as the overriding value
					// newId.setValue( wid.getValue() );
					// newId.addEncounteredOldAlias( wid );
					// FactionsPlus
					// .warn( " Config option " + newId.getInAbsoluteDottedForm()
					// + " was overwritten by old alias found for it "
					// + wid.getInAbsoluteDottedForm( virtualRoot ) );
					// }
					// } else {
					// if ( rtcid.wasAlreadyLinked() ) {// was linked to new id, meaning it was already set
					//
					// } else {
					// rtcid.linkTo( wid );
					// rtcid.setValue( wid.getValue() );
					// }
					// }
					
					
					
				} else {// non id
					assert ( currentItem instanceof WYRawButLeveledLine );
					// ignore raw lines like comments or empty lines, for now
					// currentItem = currentItem.getNext();
				}
			}// else
			
			// if (null == currentItem) {
			// WYSection par = currentItem.getParent();
			// if (null != par);
			// }
			currentItem = currentItem.getNext();
		}// inner while
			// parent = parent.getParent();
		// }// outer while
		
		// }// sync
	}
	
	private static BufferedWriter	bw;
	
	
	public final static void saveConfig() {
		try {
			// FIXME: actually meld the class options/values into the WYIdentifiers here in the represented yml file
			// before you write virtualRoot
			
			FileOutputStream fos = null;
			OutputStreamWriter osw = null;
			bw = null;
			try {
				fos = new FileOutputStream( new File( Config.fileConfig.getParent(), "config2.yml" ) );
				osw = new OutputStreamWriter( fos, Q.UTF8 );
				bw = new BufferedWriter( osw );
				// parseWrite( 0, config.getValues( false ) );
				appendSection( 0, virtualRoot );
			} catch ( IOException e ) {
				Q.rethrow( e );
			} finally {
				if ( null != bw ) {
					try {
						bw.close();
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
				if ( null != osw ) {
					try {
						osw.close();
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
				if ( null != fos ) {
					try {
						fos.close();
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
			}
			// for ( Map.Entry<String, Object> entry : config.getValues( true).entrySet() ) {
			// Object val = entry.getValue();
			// if ( !( val instanceof MemorySection ) ) {//ignore sections, parse only "var: value" tuples else it won't carry
			// over
			// String key = entry.getKey();
			// root.put(key,val);
			// }else {
			// MemorySection msVal=(MemorySection)val;
			// msVal.getValues( true );
			// }
			// }
			//
			// DumperOptions opt = new DumperOptions();
			// opt.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );
			// final Yaml yaml = new Yaml( opt );
			// FileOutputStream x = null;
			// OutputStreamWriter y=null;
			// try {
			// x=new FileOutputStream( Config.fileConfig );
			// y = new OutputStreamWriter( x, "UTF-8" );
			// yaml.dump(root,y );
			// } finally {
			// if ( null != x ) {
			// x.close();
			// }
			// }
			
			// getConfig().save( Config.fileConfig );
		} catch ( RethrownException e ) {
			e.printStackTrace();
			throw FactionsPlusPlugin.bailOut( "could not save config file: " + Config.fileConfig.getAbsolutePath() );
		}
	}
	
	private static WYSection	virtualRoot	= null;
	
	
	private final static void reloadConfig() {
		
		if ( Config.fileConfig.exists() ) {
			if ( !Config.fileConfig.isFile() ) {
				throw FactionsPlusPlugin.bailOut( "While '" + Config.fileConfig.getAbsolutePath()
					+ "' exists, it is not a file!" );
			}
			
			// config file exists
			try {
				
				// now read the existing config
				virtualRoot = WannabeYaml.read( fileConfig );
				
				
				
				// now check to see if we have any old config options or invalid ones in the config
				// remove invalids (move them to config_invalids.yml and carry over the old config values to the new ones, then
				// remove old
				// but only if new values are not already set
				synchronized ( mapField_to_ListOfWYIdentifier ) {
					mapField_to_ListOfWYIdentifier.clear();
					parseOneTime_and_CheckForValids( virtualRoot, null );
					
					
					parseSecondTime_and_sortOverrides();// from mapField_to_ListOfWYIdentifier
					// now we need to use mapField_to_ListOfWYIdentifier to see which values (first in list) will have effect
					// and notify admin on console only if the below values which were overridden have had a different value
					// coalesceOverrides( virtualRoot );
					
					// addMissingFieldsToConfig( virtualRoot );
					// TODO: when done:
					mapField_to_ListOfWYIdentifier.clear();
					
				}// sync
				
			} catch ( IOException e ) {
				e.printStackTrace();
				throw FactionsPlusPlugin.bailOut( "failed to load existing config file '" + Config.fileConfig.getAbsolutePath()
					+ "'" );
			}
			
		} else {
			// FIXME: what to do when config doesn't exit, probably just saveConfig() or fill up virtualRoot
			throw FactionsPlus.bailOut( "inexistent config" );
		}
		
		applyChanges();
		
		saveConfig();
		
		// always get defaults, we never know how many settings (from the defaults) are missing in the existing config file
		// InputStream defConfigStream = FactionsPlus.instance.getResource( Config.fileConfigDefaults );// this is the one
		// inside the .jar
		// if ( defConfigStream != null ) {
		// Config.config = YamlConfiguration.loadConfiguration( defConfigStream );
		// } else {
		// throw FactionsPlusPlugin.bailOut(
		// "There is no '"+Config.fileConfigDefaults+"'(supposed to contain the defaults) inside the .jar\n"
		// + "which means that the plugin author forgot to include it" );
		// }
		
		// if ( Config.fileConfig.exists() ) {
		// if (!Config.fileConfig.isFile()) {
		// throw FactionsPlusPlugin.bailOut( "While '"+Config.fileConfig.getAbsolutePath()+"' exists, it is not a file!");
		// }
		// // config file exists? we add the settings on top, overwriting the defaults
		// try {
		// //even though this config exists, some defaults might be new so we still need to write the config out later with
		// saveConfig();
		// YamlConfiguration realConfig = YamlConfiguration.loadConfiguration( Config.fileConfig );
		// for ( Map.Entry<String, Object> entry : realConfig.getValues( true ).entrySet() ) {
		// Object val = entry.getValue();
		// if ( !( val instanceof MemorySection ) ) {//ignore sections, parse only "var: value" tuples else it won't carry over
		// // FactionsPlus.info( entry.getKey()+ " ! "+val );
		// String key = entry.getKey();
		// // if (Config.config.contains( key)) {
		// // //we don't want to overwrite the key cause it may be different case, funnily enough this shouldn't matter but it
		// freaking does
		// // if (str_economyCostToAnnounce.equalsIgnoreCase( key )) {
		// //// Config.config.get
		// // if (!str_economyCostToAnnounce.equals(key)) {
		// // System.out.println(key+"+"+str_economyCostToAnnounce+"+"+config.get(str_economyCostToAnnounce));
		// //// DumperOptions options = new DumperOptions();
		// //// options.setDefaultFlowStyle( FlowStyle.BLOCK1 )
		// // throw FactionsPlus.bailOut( "");
		// //
		// // }
		// // }
		// // }else {
		// // FIXME: temp Config.config.set( key,val );// overwrites existing defaults already in config
		// // }
		// // FactionsPlus.info( ""+config.get(entry.getKey())+"/2/"+config.getInt( str_economyCostToAnnounce));
		// // FactionsPlus.info(str_economyCostToAnnounce+"//"+entry.getKey()+"//"+ config.get(str_economyCostToAnnounce));
		// }
		// }
		// } catch ( Exception e ) {
		// e.printStackTrace();
		// throw FactionsPlusPlugin.bailOut( "failed to load existing config file '"+Config.fileConfig.getAbsolutePath()+"'");
		// }
		// }else {
		// FactionsPlusPlugin.info(Config.fileConfig+" did not previously exist, creating a new config using defaults from the .jar");
		// }
		//
		// saveConfig();
	}
	
	
	private static void applyChanges() {
		virtualRoot.recalculateLineNumbers();
		parseAndApplyChanges( virtualRoot );// ,null);//like setting lines as comments due to duplicates/invalid/overridden
	}
	
	
	private static void parseAndApplyChanges( WYSection root ) {// , String dottedParentSection) {
		assert Q.nn( root );
		WYItem<COMetadata> currentItem = root.getFirst();
		// boolean isTopLevelSection = ( null == dottedParentSection ) || dottedParentSection.isEmpty();
		
		while ( null != currentItem ) {
			
			Class<? extends WYItem> cls = currentItem.getClass();
			
			
			if ( WYSection.class == cls ) {
				WYSection cs = (WYSection)currentItem;
				// String dotted = ( isTopLevelSection ? cs.getId() : dottedParentSection + Config.DOT + cs.getId() );
				assert null == cs.getMetadata() : "this should not have metadata, unless we missed something";
				parseAndApplyChanges( cs );// , dotted );// recurse
			} else {
				if ( WYIdentifier.class == cls ) {
					WYIdentifier<COMetadata> wid = ( (WYIdentifier)currentItem );
					// String dotted = ( isTopLevelSection ? wid.getId() : dottedParentSection + Config.DOT + wid.getId() );
					COMetadata meta = wid.getMetadata();
					if (null != meta) {
						//ok this one has meta, ie. it's one of duplicate/invalid/overridden
						currentItem=meta.apply();
					}
				} else {// non id
					assert ( currentItem instanceof WYRawButLeveledLine );
					// ignore raw lines like comments or empty lines, for now
					// currentItem = currentItem.getNext();
				}
			}// else
			
			// if (null == currentItem) {
			// WYSection par = currentItem.getParent();
			// if (null != par);
			// }
			currentItem = currentItem.getNext();
		}// while
	}
	
	
	private static void parseSecondTime_and_sortOverrides() {
		synchronized ( mapField_to_ListOfWYIdentifier ) {
			DualPack dualsearch = new DualPack( "", WYIdentifier.NULL );
			// parse all found config options in .yml , only those found! and sort the list for their overrides
			// which means, we'll now know what option overrides which one if more than 1 was found in .yml for a specific
			// config field
			// realAlias if found in .yml always overrides any old aliases found, else if no realAlias found, then
			// the top oldAliases override the bottom ones when looking at the @Option annotation
			
			Set<Entry<Field, TypedLinkedList<DualPack<String, WYIdentifier>>>> iterable =
				mapField_to_ListOfWYIdentifier.entrySet();
			for ( Entry<Field, TypedLinkedList<DualPack<String, WYIdentifier>>> fieldToList : iterable ) {
				Field field = fieldToList.getKey();
				Option anno = field.getAnnotation( Option.class );
				String[] orderOfAliases = anno.oldAliases_alwaysDotted();
				String realAlias = anno.realAlias_inNonDottedFormat();
				
				TypedLinkedList<DualPack<String, WYIdentifier>> list = fieldToList.getValue();
				assert null != list;
				assert list.size() > 0;
				
				TypedLinkedList<DualPack<String, WYIdentifier>> listInOverridingOrder =
					new TypedLinkedList<DualPack<String, WYIdentifier>>();
				
				
				for ( int i = 0; i < orderOfAliases.length; i++ ) {
					dualsearch.reuse( orderOfAliases[i] );
					
					DualPack<String, WYIdentifier> existing = list.getOriginal( dualsearch );
					if ( null != existing ) {
						listInOverridingOrder.addLast( existing );
						boolean was = list.remove( existing );
						assert was;
					} else {
						// we just didn't encounter this oldAlias which was defined in the annotation
						// so we skip to next
					}
				}
				// did we however also find the realAlias, if so put it at top of list
				// if (real) we can't, we don't know it's dotted format yet, we assume that whatever 1 element is left, if any
				// is the realAlias, cannot be anything else
				
				assert list.size() <= 1;
				
				if ( list.size() > 0 ) {
					// still one left, must be the realAlias
					DualPack<String, WYIdentifier> real = list.getFirst();
					DualPack<String, WYIdentifier> removed = list.removeFirst();
					assert real == removed;
//					assert real.getFirst().equals( realAlias ):real.getFirst()+" "+realAlias;yeah it's dotted vs non-dotted here, fail
					listInOverridingOrder.addFirst( real );
				}
				assert list.size() == 0;
				// list.clear();//no use keeping the memory
				fieldToList.setValue( listInOverridingOrder );// store the list of encountered old aliases in order of overrides
				
				Iterator<DualPack<String, WYIdentifier>> iter = listInOverridingOrder.iterator();
				DualPack<String, WYIdentifier> first = iter.next();
				assert null != first;// must have at least the realAlias in that list;
//				assert realAlias.equals(first);
				
				while ( iter.hasNext() ) {
					DualPack<String, WYIdentifier> overridenOne = iter.next();
					WYIdentifier<COMetadata> wid = overridenOne.getSecond();
					wid.setMetadata( new CO_Overridden( wid, overridenOne.getFirst(), first.getSecond(), first.getFirst() ) );
				}
				
				// done: in order to be able to keep accurate line numbers when reported or mixed in the comments we have to
				// mark the overridden/invalid/duplicates as such but not yet modify them, and only at the end, after we've also
				// inserted missing option, then and only then modify the lines(by transforming into comments) since by this
				// time now we'll have the line numbers correctly
				
				// for ( DualPack<String, WYIdentifier> overridingOrder : listInOverridingOrder ) {
				// overridingOrder.getFirst();
				// }
				// for ( DualPack<String, WYIdentifier> dualPack : list ) {
				// String dotted = dualPack.getFirst();
				// Field fieldForDotted = ConfigOptionName.dottedClassOptions_To_Fields.get( dotted );
				// if (field.equals( fieldForDotted )) {
				// //this 'dotted' is our realAlias but in dotted form
				// //which means it would override all config options for this field, regardless of their order in the file
				//
				// }else{
				// assert null == fieldForDotted;
				// }
				// // dualPack.getSecond();
				// int index = Utilities.getIndexOfObjectInArray( dotted, orderOfAliases );
				// }
			}
		}
	}
	
	
	// private static void coalesceOverrides( WYSection root ) {
	// synchronized ( mapField_to_ListOfWYIdentifier ) {
	// assert !mapField_to_ListOfWYIdentifier.isEmpty();
	//
	// assert Q.nn( root );
	// WYItem currentItem = root.getFirst();
	// // WYSection parent = root;
	// // int level=0;
	// // while ( null != parent ) {
	//
	// while ( null != currentItem ) {
	//
	// Class<? extends WYItem> cls = currentItem.getClass();
	//
	// if ( WYSection.class == cls ) {
	// WYSection cs = (WYSection)currentItem;
	// // sections are not checked for having oldaliases mainly since they are part of the dotted form of a config
	// // options and thus
	// // are indirectly checked when config options(aka ids) are checked
	// coalesceOverrides( cs );// recurse
	// // parent = cs;
	// // currentItem = cs.getFirst();
	// } else {
	// if ( WYIdentifier.class == cls ) {
	// WYIdentifier wid = ( (WYIdentifier)currentItem );
	// String dotted = wid.getID_InAbsoluteDottedForm( virtualRoot );
	// // System.out.println( dotted );
	//
	// // parse each of the encountered key:value in config, and
	// // check if it's overridden by one before it or by the realAlias, then comment it out if so
	// Field foundAsField = ConfigOptionName.dottedClassOptions_To_Fields.get( dotted );
	// if ( null == foundAsField ) {
	// throw FactionsPlus
	// .bailOut( "this should not happen at this time, cause previous code took care of it?" );
	// } else {
	// TypedLinkedList<DualPack<String, WYIdentifier>> list =
	// mapField_to_ListOfWYIdentifier.get( foundAsField );
	// FactionsPlus.warn( ChatColor.YELLOW + dotted + " " + list.size() );
	// // // TODO: make sure here the order in which we check is the ordered that we encountered them in
	// // file
	// // Set<Entry<Field, TypedLinkedList<DualPack<String, WYIdentifier>>>> iterable =
	// // list.entrySet();
	// // for ( DualPack<String, WYIdentifier> configOption_Field : list )
	// // {
	// // Field field = configOption_Field.getKey();
	// // Option fieldAnno = field.getAnnotation( Option.class );
	// // assert null != fieldAnno;
	// // String realAlias_undotted = fieldAnno.realAlias_inNonDottedFormat();
	// // // realAliasDotted=field.get
	// // 1
	// // FactionsPlus.warn( realAlias_undotted );// +" / "+realAliasDotted);
	// // TypedLinkedList<DualPack<String, WYIdentifier>> value = configOption_Field.getValue();
	// // assert value.size() == 1;
	// for ( DualPack<String, WYIdentifier> aliasesEncountered : list ) {
	// String dottedFormOfField = aliasesEncountered.getFirst();
	// // TODO: must find better way
	// int dotEndsAt = dottedFormOfField.lastIndexOf( Config.DOT ) + 1;
	// assert dotEndsAt < dottedFormOfField.length();
	// String undottedField =
	// dotEndsAt >= 0 ? dottedFormOfField.substring( dotEndsAt ) : dottedFormOfField;
	//
	// WYIdentifier realOrOldAliasAndValue = aliasesEncountered.getSecond();// if real=>undotted;
	// // else it's dotted
	//
	// FactionsPlus.info( ChatColor.YELLOW + undottedField + ": " + ChatColor.RED
	// + realOrOldAliasAndValue );
	// // if ( aliasesEncountered.getFirst().equals( realAlias_undotted ) ) {
	// // //
	// // }
	// }// for
	// System.out.println( "." );
	//
	// // }
	// }// else
	// } else {// non id
	// assert ( currentItem instanceof WYRawButLeveledLine );
	// // ignore raw lines like comments or empty lines, for now
	// // currentItem = currentItem.getNext();
	// }
	// }// else
	//
	// // if (null == currentItem) {
	// // WYSection par = currentItem.getParent();
	// // if (null != par);
	// // }
	// currentItem = currentItem.getNext();
	// }// inner while
	// // parent = parent.getParent();
	// // }// outer while
	// }// sync
	// }
	
	
	public static boolean isInited() {
		return inited;
	}
	
}
