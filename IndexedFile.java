
public class IndexedFile {
    private Disk disk; // Disk on which the file will be written
    private char[] buffer; // Disk buffer
    private int recordSize; // In characters
    private int keySize; // In characters
    private int indexRecordSize; // In characters
    // Fields describing data portion of file
    private int recordsPerSector; // sectorSize/recordSize
    private int firstAllocated; // Sector number where data begins
    private int sectorsAllocated; // Sectors originally allocated for data
    private int overflowStart; // Sector number where overflow begins
    private int overflowSectors; // Count of overflow sectors in use
    // Fields describing index portion of file
    private int indexStart; // Sector number where index begins
    private int indexSectors; // Number of sectors allocated for index
    private int indexRoot; // Sector number of root of index
    private int indexLevels; // Number of levels of index

    public IndexedFile(Disk disk, int recordSize, int keySize, int indexRecordSize, int firstAllocated, // Default
													// ctor
	    int indexStart, int indexSectors, int indexRoot, int indexLevels) {
	this.disk = disk;
	this.buffer = new char[disk.getSectorSize()];
	this.recordSize = recordSize;
	this.keySize = keySize;
	this.indexRecordSize = indexRecordSize;

	this.recordsPerSector = this.disk.getSectorSize() / this.recordSize;
	this.firstAllocated = firstAllocated;

	this.indexStart = indexStart;
	this.indexSectors = indexSectors;
	this.indexRoot = indexRoot;
	this.indexLevels = indexLevels;

	this.overflowSectors = 0;
    }

    public boolean insertRecord(char[] record) {
	char[] key = getKey(record, 0); // Get key array
	int sectorNum = getSector(convertToString(key)); // Get sector number
	disk.readSector(sectorNum, buffer); // Read the sector to the buffer
	boolean notDuplicate = checkNotDuplicate(buffer, key); // Check
							       // duplicate key

	if (notDuplicate) // If its not a duplicate key
	{
	    boolean availableSpace = findAvailableAndStore(buffer, record);
	    if (availableSpace) // Found available space
	    {
		disk.writeSector(sectorNum, buffer); // Write the buffer to disk
		return true;
	    } else // Need to go overflow sectors
	    {
		if (overflowSectors == 0) // Oops i don't have a overflow sector
		{
		    overflowStart = this.indexStart + this.indexSectors;
		    disk.readSector(overflowStart, buffer);
		    findAvailableAndStore(buffer, record);
		    disk.writeSector(overflowStart, buffer);
		    overflowSectors++; // Increment number of overflow sectors
		    return true;
		} else // If not the first time
		{
		    int currOverflowSector = overflowStart;
		    while ((currOverflowSector < overflowStart + overflowSectors) && notDuplicate) {
			disk.readSector(currOverflowSector, buffer);
			notDuplicate = checkNotDuplicate(buffer, key);
			currOverflowSector++; // Move to next overflow sector
		    }
		    if (notDuplicate) // No duplicate key in overflow sector
		    {
			currOverflowSector--; // Back to current overflow sector
			availableSpace = findAvailableAndStore(buffer, record);
			if (!availableSpace) // If not more space in this
			{
			    currOverflowSector++;
			    overflowSectors++; // Increment number of overflow
					       // sectors in use
			    disk.readSector(currOverflowSector, buffer);
			    findAvailableAndStore(buffer, record);
			}
			disk.writeSector(currOverflowSector, buffer);
			return true;
		    } else // Found duplicate key in overflow sector
			return false;
		}
	    }
	} else // Same key is found no further action, return false
	    return false;
    }

    public boolean findRecord(char[] record) {
	char[] key = getKey(record, 0); // Get key array
	int sectorNum = getSector(convertToString(key)); // Get sector number
	disk.readSector(sectorNum, buffer); // Read the sector to the buffer

	int numKeysChecked = 0; // Count number of key we checked
	int nextKeyPlace = 0; // Index of next key
	int compVal = 1;

	while ((numKeysChecked < recordsPerSector) && (buffer[nextKeyPlace] != 0) && compVal != 0) {
	    char[] tempKey = getKey(buffer, nextKeyPlace); // Get keys from
							   // buffer
	    compVal = compareKeys(key, tempKey); // Compare key
	    numKeysChecked++; // Increment number of keys checked
	    nextKeyPlace = nextKeyPlace + recordSize; // Move to next key place
	}
	if (compVal == 0) // If found
	{
	    char[] tmpRecord = getRecord(buffer, nextKeyPlace - recordSize);
	    copyRecord(tmpRecord, record); // Copy the entire found record into
					   // parameter record
	    return true;
	} else if ((compVal != 0) && (numKeysChecked < recordsPerSector))
	    return false;
	else // If not found and sector is full, we need to check overflow
	     // sectors
	{
	    int currOverflowSector = overflowStart; // First overflow sector
	    numKeysChecked = 0; // Count number of key we checked
	    nextKeyPlace = 0; // Index of next key
	    compVal = 1;

	    while ((currOverflowSector < overflowStart + overflowSectors) && compVal != 0) {
		disk.readSector(currOverflowSector, buffer); // Read in overflow
							     // sector to buffer
		while ((numKeysChecked < recordsPerSector) && (buffer[nextKeyPlace] != 0) && compVal != 0) {
		    char[] tempKey = getKey(buffer, nextKeyPlace); // Get keys
								   // from
								   // buffer
		    compVal = compareKeys(key, tempKey); // Compare key
		    numKeysChecked++; // Increment number of keys checked
		    nextKeyPlace = nextKeyPlace + recordSize; // Move to next
							      // key place
		}
		currOverflowSector++; // Move to next overflow sector
	    }
	    if (compVal != 0) // Not found in overflow sector
		return false;
	    else {
		char[] tmpRecord = getRecord(buffer, nextKeyPlace - recordSize);
		copyRecord(tmpRecord, record); // Copy the entire found record
					       // into parameter record
		return true;
	    }
	}
    }

    private int getSector(String key) // Returns sector number indicated by key
    {
	boolean notFound = true; // Not found the target sector
	boolean notDoneCurrSector = true; // Not done with comparing the current
					  // sector
	char[] keyChar = new char[keySize];
	key.getChars(0, key.length(), keyChar, 0); // Convert the key to
						   // characters
	char[] currSector = new char[disk.getSectorSize()];
	int address = indexRoot; // Start from root sector
	int currLevel = 0; // Count current tree level

	while (notFound) {
	    disk.readSector(address, currSector); // Read sector to buffer
	    int currKeyPlace = 0; // First index of the KeyOne
	    notDoneCurrSector = true; // Reset notDone

	    while (notDoneCurrSector) {
		boolean lastKeyInSector = false; // is the last key in sector
		int compVal = 0;
		int nextKeyPlace = currKeyPlace + indexRecordSize; // Get next
								   // Key place
		if (nextKeyPlace >= disk.getSectorSize() || currSector[nextKeyPlace] == 0)
		    lastKeyInSector = true;
		else
		    compVal = compareKeys(keyChar, getKey(currSector, nextKeyPlace));

		if (compVal == -1 || lastKeyInSector) {
		    address = getSectorNumber(currSector, currKeyPlace);
		    notDoneCurrSector = false;
		    currLevel++; // Increment leve;
		} else // Else if equals or greater than next key
		    currKeyPlace = nextKeyPlace; // Set current key equals to
						 // next key
	    }
	    if (currLevel == indexLevels) // When we get to bottom tree level
		notFound = false; // We done, we found the sector number
	}

	return address;
    }

    private int compareKeys(char[] keyOne, char[] keyTwo) // Compare two keys in
							  // char array.
    { // Return 1: key1 > key2, 0: key1 == key2, -1: key1 < key2
	boolean allSame = false; // Both keys are the same
	boolean keyOneLarger = false; // Key 1 larger
	boolean keyTwoLarger = false; // Key 2 larger
	boolean keyOneDone = false; // End of comparing key 1
	boolean keyTwoDone = false; // End of comparing key 2
	int countKeyChars = 0; // To count characters in keys

	while (!allSame && !keyOneLarger && !keyTwoLarger) {
	    char charKeyOne = 0;
	    char charKeyTwo = 0;

	    if ((countKeyChars < keyOne.length) && (keyOne[countKeyChars] != 0))
		charKeyOne = keyOne[countKeyChars];
	    else // If not we done with comparing keyOne
		keyOneDone = true;
	    if ((countKeyChars < keyTwo.length) && (keyTwo[countKeyChars] != 0))
		charKeyTwo = keyTwo[countKeyChars];
	    else // If not we done with comparing keyTwo
		keyTwoDone = true;

	    if (keyOneDone && keyTwoDone) // If both keys are the same
		allSame = true;
	    else if (keyOneDone) // Else if keyOne has no more element
		keyTwoLarger = true; // Making keyTwo larger
	    else if (keyTwoDone) // Else if keyTwo has no more element
		keyOneLarger = true; // Making keyOne larger
	    else // Else we compare both elements
	    {
		if (charValue(charKeyOne) > charValue(charKeyTwo))
		    keyOneLarger = true;
		else if (charValue(charKeyTwo) > charValue(charKeyOne))
		    keyTwoLarger = true;
	    } // else both same we do nothing, and check next char
	    countKeyChars++;
	}
	if (allSame)
	    return 0;
	else if (keyOneLarger)
	    return 1;
	else
	    return -1;
    }

    private int charValue(char ch){
	int val = Character.valueOf(ch);
	if (val >= 97 && val <= 122)
	    val -= 32;
	return val;
    }

    private char[] getKey(char[] sector, int begin) {
	char[] key = new char[keySize]; // Create new empty key array to return
	for (int i = 0; i < keySize; i++)
	    key[i] = sector[begin + i]; // Copy key
	return key;
    }

    private char[] getRecord(char[] sector, int begin) {
	char[] record = new char[recordSize];
	for (int i = 0; i < recordSize; i++)
	    record[i] = sector[begin + i];
	return record;
    }

    private void copyRecord(char[] recordOne, char[] recordTwo) {
	for (int i = 0; i < recordSize; i++)
	    recordTwo[i] = recordOne[i];
    }

    private int getSectorNumber(char[] sector, int keyBegin) // Get the sector
							     // number after a
							     // key
    {
	int sectorNum = 0; // Sector Number
	int numChar = 0; // Count number of characters are there to represent
			 // sector number
	int sectorNumStart = keyBegin + keySize; // Place where sector Number
						 // starts in a indexRecord
	int sectorNumSize = indexRecordSize - keySize; // Sector number size
	StringBuilder sectorNumS = new StringBuilder();

	for (int i = sectorNumStart; (numChar < sectorNumSize) && (sector[i] != 0); i++) {
	    sectorNumS.append(sector[i]);
	    numChar++;
	}
	sectorNum = Integer.parseInt(sectorNumS.toString());
	return sectorNum;
    }

    private String convertToString(char[] characters) { // Convert character
							// array to String
	return new String(characters);
    }

    private boolean checkNotDuplicate(char[] buffer, char[] key) // Check
								 // duplicate
								 // key error
    {
	int numKeysChecked = 0; // Count number of key we checked
	int nextKeyPlace = 0; // Index of next key
	int compVal = 1;

	while ((numKeysChecked < recordsPerSector) && (buffer[nextKeyPlace] != 0) && compVal != 0) {
	    char[] tempKey = getKey(buffer, nextKeyPlace);
	    compVal = compareKeys(key, tempKey); // Compare key
	    numKeysChecked++; // Increment number of keys checked
	    nextKeyPlace = nextKeyPlace + recordSize; // Move to next key place
	}
	if (compVal == 0) // If found duplicated key
	    return false;
	else // Not found duplicated key
	    return true;
    }

    private boolean findAvailableAndStore(char[] buffer, char[] record) {
	int numKeys = 0; // Count number of key
	int nextKeyPlace = 0; // Next key index
	boolean notFoundSpace = true; // Not found available space

	while ((numKeys < recordsPerSector) && (notFoundSpace)) {
	    if (buffer[nextKeyPlace] == 0) // If found available space
		notFoundSpace = false;
	    else // If not
	    {
		numKeys++; // Increment number of keys
		nextKeyPlace = nextKeyPlace + recordSize; // Move to next key
							  // place
	    }
	}
	if (notFoundSpace) // If not found available space
	    return false;
	else // If found space in buffer, we store there.
	{
	    for (int i = 0; i < recordSize; i++) // Store record
	    {
		buffer[nextKeyPlace] = record[i];
		nextKeyPlace++;
	    }
	    return true;
	}
    }

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<DELETE THIS FUNCTION WHEN
    // DONE>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    public int getSectorPublic(String key) // Returns sector number indicated by
					   // key
    {
	boolean notFound = true; // Not found the target sector
	boolean notDoneCurrSector = true; // Not done with comparing the current
					  // sector
	char[] keyChar = new char[keySize];
	key.getChars(0, key.length(), keyChar, 0); // Convert the key to
						   // characters
	char[] currSector = new char[disk.getSectorSize()];
	int address = indexRoot; // Start from root sector
	int currLevel = 0; // Count current tree level

	while (notFound) {
	    disk.readSector(address, currSector); // Read sector to buffer
	    int currKeyPlace = 0; // First index of the KeyOne
	    notDoneCurrSector = true; // Reset notDone

	    while (notDoneCurrSector) {
		boolean lastKeyInSector = false; // is the last key in sector
		int compVal = 0;
		int nextKeyPlace = currKeyPlace + indexRecordSize; // Get next
								   // Key place
		if (nextKeyPlace >= disk.getSectorSize() || currSector[nextKeyPlace] == 0)
		    lastKeyInSector = true;
		else
		    compVal = compareKeys(keyChar, getKey(currSector, nextKeyPlace));
		if (compVal == -1 || lastKeyInSector) {
		    address = getSectorNumber(currSector, currKeyPlace);
		    notDoneCurrSector = false;
		    currLevel++; // Increment leve;
		} else // Else if equals or greater than next key
		    currKeyPlace = nextKeyPlace;
	    }
	    if (currLevel == indexLevels) // When we get to bottom tree level
		notFound = false; // We done, we found the sector number
	}
	return address;
    }
}
