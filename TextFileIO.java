import java.util.Scanner;
import java.io.*;
import java.util.regex.*;

/**
 * @author rt
 *
 */
public class TextFileIO {
    private static int dataSectorsAllocated = 0;
    private static int indexStart;;
    private static int indexSectors = 0;
    private static int indexRoot;
    private static int indexLevels = 0;

    public static void loadMountainData(String fileName, Disk disk, int firstAllocated, int recordSize, int nameSize,
	    int countrySize) {
	int recordsPerSector = disk.getSectorSize() / recordSize;
	int currentSector = firstAllocated; // Current sector in this case start
					    // 1000
	int currRecordsInSector = 0; // Number of records in the current sector
	char[] records = new char[disk.getSectorSize()];
	Scanner textFile = null;

	try // Check open File error
	{
	    textFile = new Scanner(new File(fileName));
	} catch (FileNotFoundException e) {
	    System.out.println("File not found");
	    System.out.println("or could not be opened.");
	    System.exit(1);
	}

	while (textFile.hasNextLine()) // Check if is end of file
	{
	    String line = textFile.nextLine();// Read in one line from file

	    if (currRecordsInSector < (recordsPerSector - 3))
	    {
		processString(currRecordsInSector * recordSize, records, line, nameSize, countrySize); // Format
	    }
	    else // Need to leave 3 RecordSpaces, if greater than or equals to
		 // recordsPerSector-3
	    {
		disk.writeSector(currentSector, records); // Write the records
							  // into current sector
							  // in disk
		dataSectorsAllocated++; // Increment the number of sectors
					// allocated
		currentSector++; // increment current Sector. Ex: 1000 -> 1001
		currRecordsInSector = 0; // Reset the number of record in
					 // current sector
		records = new char[disk.getSectorSize()]; // Reset the record
							  // array;
		processString(currRecordsInSector * recordSize, records, line, nameSize, countrySize);// Format
												      // the
												      // record
	    } // recordBegin : number of records in sector * record size

	    if (!(textFile.hasNextLine())) // This will be the case that at the
					   // very end of the file.If the number
					   // of records is less
	    { // than recordsPerSector -3 than the last few records will never
	      // go into else statement.
		disk.writeSector(currentSector, records); // Write the records
							  // into current sector
							  // in disk
		dataSectorsAllocated++; // Increment the number of sectors
					// allocated
	    }
	    currRecordsInSector++;
	}
    }

    private static void processString(int recordBegin, char[] records, String line, int nameSize, int countrySize) {
	String name = "", country = "", altitude = "";
	String regularExpression = "(.*)[#](.*)[#](\\d*)";// Use Regular
							  // Expression to take
							  // out #, and separate
							  // the name country
							  // and altitude
	Pattern p = Pattern.compile(regularExpression);
	Matcher m = p.matcher(line);
	if (m.matches()) {
	    name = m.group(1); // select the groups
	    country = m.group(2);
	    altitude = m.group(3);
	}
	// Put the character content of Strings into character arrays
	// recordBegin : number of records in sector * record size
	name.getChars(0, name.length(), records, recordBegin);// DestBegin:
							      // recordBegin
	country.getChars(0, country.length(), records, recordBegin + nameSize);// DestBegin:
									       // recordBegin
									       // +
									       // 27
	altitude.getChars(0, altitude.length(), records, recordBegin + nameSize + countrySize);// DestBegin:
											       // recordBegin
											       // +
											       // 27
											       // +
											       // 27
    }

    public static void buildIndexTree(Disk myDisk, int keySize, int firstAllocated, int sectorNumSize) // Multi-way
												       // tree
												       // -
												       // Index
    {
	indexStart = firstAllocated + dataSectorsAllocated; // IndexStart is the
							    // sum of
							    // firstAllocated
							    // and
							    // sectorsAllocated
	char[] currKeysSector = new char[myDisk.getSectorSize()]; // Hold all
								  // the keys in
								  // a index
								  // sector

	int currIndexSector = indexStart; // Current index sector that we are
					  // working on in the disk array
	int currDataSector = firstAllocated; // Current Data Sector we working
					     // on, beginning at 1000
	int levelEnd = indexStart - 1; // Current tree level end, at first is
				       // last one in data sector

	int keysPerSector = (myDisk.getSectorSize()) / (keySize + sectorNumSize); // Number
										  // of
										  // key
										  // per
										  // index
										  // sector
	int currPlaceInSector; // To count where we are in a index sector array
	String sectorNumS = ""; // To hold sector number in String
	char[] sectorNumC; // To hold sector number in characters
	int currNumKeys = 0; // Number of keys in the current index sector

	int numSectorsInLevel = 0; // Number of index sectors in the level

	while (numSectorsInLevel != 1) // When only one key in sector, it is
				       // root then we end loop
	{
	    numSectorsInLevel = 0; // Reset number of sector in level
	    currPlaceInSector = 0; // Initialize current place in index sector
				   // to 0

	    while (currDataSector <= levelEnd) {
		char[] buffer = new char[myDisk.getSectorSize()]; // Empty
								  // buffer to
								  // read data
								  // sector
		myDisk.readSector(currDataSector, buffer); // Read in the
							   // sectors

		sectorNumC = new char[sectorNumSize]; // Initialize sector
						      // number in characters

		// Read in the keys into one keySector
		for (int i = 0; i < keySize; i++) {
		    currKeysSector[currPlaceInSector] = buffer[i]; // Copy the
								   // key from
								   // buffer to
								   // keysSector
		    currPlaceInSector++; // Increment current place
		}
		sectorNumS = Integer.toString(currDataSector); // Convert sector
							       // number to
							       // String
		sectorNumS.getChars(0, sectorNumS.length(), sectorNumC, 0); // Convert
									    // sector
									    // number
									    // to
									    // char
		for (int i = 0; i < sectorNumSize; i++) {
		    currKeysSector[currPlaceInSector] = sectorNumC[i]; // Copy
								       // sector
								       // number
								       // to
								       // index
								       // Sector
		    currPlaceInSector++; // Increment current place
		}
		currNumKeys++; // Increment number of key in index sector

		// Check conditions
		if ((currNumKeys == keysPerSector) || (currDataSector == levelEnd))// If
										   // index
										   // sector
										   // is
										   // full
										   // or
										   // end
										   // of
										   // level
		{
		    myDisk.writeSector(currIndexSector, currKeysSector); // Write
									 // this
									 // key
									 // sector
									 // into
									 // index
									 // sector
		    currIndexSector++; // Increment the indexSector counter
		    currKeysSector = new char[myDisk.getSectorSize()]; // Reset
								       // currKeysSector
		    currPlaceInSector = 0; // Reset current place in a sector
		    currNumKeys = 0; // Reset current number of key in index
				     // sector
		    numSectorsInLevel++; // We increment the number of sectors
					 // in this level
		}
		currDataSector++; // Increment current data sector place
	    }
	    levelEnd = currIndexSector - 1;
	    indexLevels++;
	}
	indexSectors = currIndexSector - indexStart;
	indexRoot = currIndexSector - 1;
    }

    public static int getDataSectorsAllocated() {
	return dataSectorsAllocated;
    }

    public static int getIndexStart() {
	return indexStart;
    }

    public static int getIndexSectors() {
	return indexSectors;
    }

    public static int getIndexRoot() {
	return indexRoot;
    }

    public static int getIndexLevels() {
	return indexLevels;
    }

    /**
     * @param name
     * @param nSize
     * @param country
     * @param cSize
     * @param altitude
     * @param aSize
     * @return Character Array 
     */
    public static char[] formatRecord(String name, int nSize, String country, int cSize, String altitude, int aSize) {
	int recordSize = nSize + cSize + aSize; // Get record size
	char[] nameC = new char[nSize];
	char[] countryC = new char[cSize];
	char[] altitudeC = new char[aSize];
	char[] record = new char[recordSize];

	if (name.length() < nSize) // If name length less than nSize
	    name.getChars(0, name.length(), nameC, 0); // Convert to characters
	else
	    name.getChars(0, nSize, nameC, 0); // Convert to characters
	if (country.length() < cSize)
	    country.getChars(0, country.length(), countryC, 0);
	else
	    country.getChars(0, cSize, countryC, 0);
	if (altitude.length() < aSize)
	    altitude.getChars(0, altitude.length(), altitudeC, 0);
	else
	    altitude.getChars(0, aSize, altitudeC, 0);

	for (int i = 0; i < recordSize; i++) // Copy name, country, and altitude
					     // to record array
	{
	    if (i < nSize)
		record[i] = nameC[i];
	    else if (i < (nSize + cSize))
		record[i] = countryC[i - nSize];
	    else
		record[i] = altitudeC[i - (nSize + cSize)];
	}
	return record;
    }

    /**
     * @param record
     * @param nSize
     * @param cSize
     * @param aSize
     */
    public static void printRecord(char[] record, int nSize, int cSize, int aSize) {
	StringBuilder name = new StringBuilder();
	StringBuilder country = new StringBuilder();
	StringBuilder altitude = new StringBuilder();
	int recordSize = nSize + cSize + aSize; // Get record size

	for (int i = 0; i < recordSize; i++) // Create data strings
	{
	    if (i < nSize)
		name.append(record[i]);
	    else if (i < (nSize + cSize))
		country.append(record[i]);
	    else
		altitude.append(record[i]);
	}

	System.out.println(name + ", Country: " + country + ", Altitude: " + altitude + " ft.");
    }
}
