
// Renzo Tejada

import java.util.Scanner;

public class Application {

    private static Disk myDisk;

    public static void main(String[] args) {

	myDisk = new Disk();

	TextFileIO.loadMountainData("/Users/renzotejada/git/IndexedFileDisk/mountains.txt", myDisk, 1000, 60, 27, 27);
	TextFileIO.buildIndexTree(myDisk, 27, 1000, 7);
	IndexedFile myIndexFile = new IndexedFile(myDisk, 60, 27, 34, 1000, TextFileIO.getIndexStart(),
		TextFileIO.getIndexSectors(), TextFileIO.getIndexRoot(), TextFileIO.getIndexLevels());

	Scanner keyboard = new Scanner(System.in);
	int input;
	boolean notDone = printMenuInterface(myIndexFile, keyboard);

	System.out.println("Number of data sectors allocated: " + TextFileIO.getDataSectorsAllocated());
	System.out.println("Number of index sectors allocated: " + TextFileIO.getIndexSectors());
	System.out.println("Index tree levels: " + TextFileIO.getIndexLevels());
	System.out.println("Index tree root: " + TextFileIO.getIndexRoot());
	// Scanner keyboard = new Scanner(System.in);
	int number = 0;
	int keyORnum = 0;
	// boolean notDone = true;
	boolean keyNotDone = true;
	boolean numNotDone = true;
	String key = "";

	while (notDone) {
	    System.out.print("Key(1) | Sector Number (2) | insert Record(3): ");
	    keyORnum = keyboard.nextInt();
	    switch (keyORnum) {
	    case 1:
		keyNotDone = true;
		keyboard.nextLine();
		while (keyNotDone) {
		    System.out.print("Sector key: ");
		    key = keyboard.nextLine();
		    if (key.equals("0"))
			keyNotDone = false;
		    else
			System.out.println("IndexSector: " + myIndexFile.getSectorPublic(key));
		}
		break;
	    case 2:
		numNotDone = true;
		while (numNotDone) {
		    System.out.print("Sector Number: ");
		    number = keyboard.nextInt();
		    System.out.print("Data sector: 1|2, Index sector: 3: ");
		    int nullNum = keyboard.nextInt();
		    if (nullNum == 0)
			numNotDone = false;
		    else
			myDisk.printDiskSectors(number, nullNum);
		}
		break;
	    case 3:
		keyboard.nextLine();
		String name, country, altitude;
		char[] nameC = new char[27];
		char[] countryC = new char[27];
		char[] altitudeC = new char[6];
		System.out.println("Name: ");
		name = keyboard.nextLine();
		System.out.println("Country: ");
		country = keyboard.nextLine();
		System.out.println("Altitude: ");
		altitude = keyboard.nextLine();
		name.getChars(0, name.length(), nameC, 0);
		country.getChars(0, country.length(), countryC, 0);
		altitude.getChars(0, altitude.length(), altitudeC, 0);
		char[] record = new char[60];
		for (int i = 0; i < 60; i++) {
		    if (i < 27)
			record[i] = nameC[i];
		    else if (i < 54)
			record[i] = countryC[i - 27];
		    else
			record[i] = altitudeC[i - 54];
		}
		myIndexFile.insertRecord(record);
		break;
	    }

	}

    }

    private static boolean printMenuInterface(IndexedFile myIndexFile, Scanner keyboard) {
	int input;
	boolean notDone = true;

	String menu = "\n _______________________________\n" + "|  ___________________________  |\n"
		+ "| |(1) Insert new record      | |\n" + "| |(2) Find record            | |\n"
		+ "| |(3) Quit                   | |\n" + "|  ---------------------------  |\n"
		+ " -------------------------------\n" + "-> ";
	while (notDone) {
	    System.out.print(menu); // Menu
	    input = keyboard.nextInt();

	    switch (input) {
	    case 1:
		keyboard.nextLine(); // Next line character
		String name, country, altitude;
		char[] record = new char[60];
		boolean success = false;

		System.out.println("Name: "); // Get record data
		name = keyboard.nextLine();
		System.out.println("Country: ");
		country = keyboard.nextLine();
		System.out.println("Altitude: ");
		altitude = keyboard.nextLine();

		record = TextFileIO.formatRecord(name, 27, country, 27, altitude, 6);// Format
										     // a
										     // 60
										     // character
										     // record
										     // with
										     // data
		success = myIndexFile.insertRecord(record);
		if (success)
		    System.out.println("<<< Insertion was successful >>>");
		else
		    System.out.println("<<< Found duplicate record >>>");
		break;
	    case 2:
		keyboard.nextLine(); // Next line character
		System.out.println("Name: "); // Get record data
		name = keyboard.nextLine();
		record = TextFileIO.formatRecord(name, 27, "", 27, "", 6); // Format
									   // a
									   // 60
									   // character
									   // record
									   // with
									   // key
		success = myIndexFile.findRecord(record);
		if (success)
		    TextFileIO.printRecord(record, 27, 27, 6);
		else
		    System.out.println("<<< Sorry, No record is found >>>");
		break;
	    case 3:
		notDone = false; // Quit
		break;
	    }
	}
	return notDone;
    }

}
