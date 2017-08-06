
public class Disk {
    private int sectorCount; // Sectors on the disk
    private int sectorSize; // Characters in a sector
    private char[][] store; // All disk data is stored here

    public Disk() // Default ctor
    {
	this.sectorCount = 10000; // Default size is 10,000 sectors
	this.sectorSize = 512; // Default size is 512 characters
	store = new char[this.sectorCount][this.sectorSize]; // Initialize Disk
							     // spaces
    }

    public Disk(int sectorCount, int sectorSize) // Explicit ctor
    {
	this.sectorCount = sectorCount; // Initialize sectorCount
	this.sectorSize = sectorSize; // Initialize sectorSize
	store = new char[this.sectorCount][this.sectorSize]; // Initialize Disk
							     // spaces
    }

    public void readSector(int sectorNumber, char[] buffer) { // Sector to
							      // buffer
	for (int i = 0; i < buffer.length; i++) // Copy the contents of the
						// sector whose
	    buffer[i] = store[sectorNumber][i]; // number is the first parameter
						// to the
    } // character array which is the second argument

    public void writeSector(int sectorNumber, char[] buffer) { // Buffer to
							       // sector
	for (int i = 0; i < buffer.length; i++) // Copy the contents of disk
						// buffer
	    store[sectorNumber][i] = buffer[i]; // to the sector whose number is
						// the
    } // first argument

    public int getSectorCount() { // Return number of sectors on the disk
	return this.sectorCount;
    }

    public int getSectorSize() { // Return number of characters in a sector
	return this.sectorSize;
    }

    public void printDiskSectors(int number, int nullNum) {
	int j = 0;
	if (nullNum == 1) {
	    while (j < 8) {
		for (int i = 0; i < 60; i++) {
		    System.out.print(store[number][i + (j * 60)]);
		}
		System.out.println();
		j++;
	    }
	} else if (nullNum == 2) {
	    int countChar = 0;
	    int countNull = 0;
	    for (int i = 0; i < 512; i++) {
		if (store[number][i] != '\000') {
		    countChar++;
		    System.out.println(countChar + ": " + store[number][i]);
		    countNull = countChar;
		} else if (store[number][i] == '\000') {
		    countChar = 0;
		    countNull++;
		    System.out.println("NULL " + (countNull));
		}
	    }
	} else if (nullNum == 3) // Print index sector
	{
	    int x = 0, y = 0;
	    for (x = 0; x < 15; x++) {
		for (y = 0; y < 34; y++)
		    System.out.print(store[number][y + (x * 34)]);
		System.out.println();
	    }
	}
    }
}
