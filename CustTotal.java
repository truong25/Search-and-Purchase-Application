/*============================================================================
CustTotal: A JDBC APP to list total sales for a customer from the YRB DB.

Parke Godfrey
2013 March 26 [revised]
2004 March    [original]
============================================================================*/

import java.util.*;
import java.net.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import java.sql.*;
import java.util.Scanner;

/*============================================================================
CLASS CustTotal
============================================================================*/

public class CustTotal {
    private Connection conDB;   // Connection to the database system.
    private String url;         // URL: Which database?

    private Integer custID;     // Who are we tallying?
    private String  custName;   // Name of that customer.
	private String title = "";       // Name of book title being purchased
	private String bookYear = "";
	
	private	String newName = "";
	private	String newCity = "";	
	
	private	String book = "";
	private	String category = "";
	
	private String year = "";
	private	String weight = "";
	private	String language = "";
	private String offer = "";
	private String city;
	boolean catBookChecker = false;
	
	ArrayList<String> categoryList;
	ArrayList<String> booksInList;
    // Constructor
    public CustTotal (String[] args) {
        // Set up the DB connection.
        try {
            // Register the driver with DriverManager.
            Class.forName("com.ibm.db2.jcc.DB2Driver").newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(0);
        }

        // URL: Which database?
        url = "jdbc:db2:c3421a";

        // Initialize the connection.
        try {
            // Connect with a fall-thru id & password
            conDB = DriverManager.getConnection(url);
        } catch(SQLException e) {
            System.out.print("\nSQL: database connection error.\n");
            System.out.println(e.toString());
            System.exit(0);
        }    

        // Let's have autocommit turned off.  No particular reason here.
        try {
            conDB.setAutoCommit(false);
        } catch(SQLException e) {
            System.out.print("\nFailed trying to turn autocommit off.\n");
            e.printStackTrace();
            System.exit(0);
        }    

        // Who are we tallying?
        if (args.length != 1) {
            // Don't know what's wanted.  Bail.
            System.out.println("\nUsage: java CustTotal cust#");
            System.exit(0);
        } else {
            try {
                custID = new Integer(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("\nUsage: java CustTotal cust#");
                System.out.println("Provide an INT for the cust#.");
                System.exit(0);
            }
        }

        // Is this custID for real?
        if(!customerCheck()) {
			while(!customerCheck()){
				System.out.print("There is no customer #");
				System.out.print(custID);
				System.out.println(" in the database.");
				System.out.println("Please enter a valid ID.");
				Scanner user_input = new Scanner(System.in);
				custID = user_input.nextInt();
				customerCheck();
			  // Once the customer is valid, find them
				if (customerCheck() == true)
				{

					find_customer();
					System.exit(0);
				}
			}
        }
        // Find the customer if the original input was correct
        find_customer();

		
		System.out.print("Would you like to update your information? [Y/N] ");
		Scanner s = new Scanner(System.in);
		String update_check1 = s.nextLine();
		
		
		System.out.println(update_check1);
		
		while (update_check1.equals("Y")){
			System.out.println("Current Information:");
			System.out.println("NAME: " + custName);
			System.out.println("CITY: " + city);
			
			
			if (update_check1.equals("Y"))
			{
				update_customer();
				//update variables from DB changes
					System.out.println("");
				System.out.println("The changes have been made.");
			
				customerCheck();
				find_customer();
			}	
			
			System.out.println("Would you like to do another update? [Y/N]");
			s = new Scanner(System.in);
			update_check1 = s.nextLine();
			
		}
		if( update_check1.equals("N"))
		{
			while (!catBookChecker) {
				
				System.out.println("");
				categoryList = new ArrayList<String>(fetch_categories());
				System.out.println("");		
				System.out.println("Please enter the category you would like to explore: ");
				Scanner s5 = new Scanner(System.in);
				category = s5.nextLine();

				
				booksInList = new ArrayList<String>( display_books());
				System.out.println("Please enter the TITLE of the book you are looking for: ");
				Scanner s6 = new Scanner(System.in);
				book = s6.nextLine();
				catBookChecker = (categoryList.contains(category) && booksInList.contains(book));
				System.out.println("");
				if (!catBookChecker)
				{	
					System.out.println("*** Invalid parameters ***");
				}						

			}
			
			find_book();
			min_price();
			
		}

        // Commit.  Okay, here nothing to commit really, but why not...
        try {
            conDB.commit();
        } catch(SQLException e) {
            System.out.print("\nFailed trying to commit.\n");
            e.printStackTrace();
            System.exit(0);
        }    
        // Close the connection.
        try {
            conDB.close();
        } catch(SQLException e) {
            System.out.print("\nFailed trying to close the connection.\n");
            e.printStackTrace();
            System.exit(0);
        }    

    }

	
	/***************
	*    Methods
	****************/
	
	
    public boolean customerCheck() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           inDB      = false;  // Return.

        queryText =
            "SELECT name     "
          + "FROM yrb_customer "
          + "WHERE cid = ?     ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("customerCheck failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setInt(1, custID.intValue());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("customerCheck failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;
                custName = answers.getString("name");
            } else {
                inDB = false;
                custName = null;
            }
        } catch(SQLException e) {
            System.out.println("customerCheck failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("customerCheck failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("customerCheck failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return inDB;
    }
	
	/**
	* find_customer
	**/

    public void find_customer() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        queryText =
            "SELECT C.city    "
          + "    FROM yrb_customer C          "
          + "    WHERE C.cid = ?             ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("find_customer failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setInt(1, custID.intValue());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("find_customer failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Variables to hold the column value(s).
		city = "";


        // Walk through the results and present them.
        try {
			if (answers.next()) {
                city = answers.getString("city");   
            } 
			System.out.println("");
            System.out.print("(#");
            System.out.print(custID);
            System.out.print(" - " + custName + " - "); 
			System.out.println(city + ")");  
			System.out.println("");
			
        } catch(SQLException e) {
            System.out.println("find_customer failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("find_customer failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("find_customer failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
		
		
    }
	/**
	* update_customer
	**/
	 public void update_customer() 
	 {
        String            queryText2 = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        int         answers   = 0;   // A cursor.
		String update_item = "";

		System.out.println("Enter the item that you would like to update: ");
		while( !(update_item.equals("NAME") || update_item.equals("CITY")) ) 
		{
			Scanner s1 = new Scanner(System.in);
			update_item = s1.nextLine();
			if (update_item.equals("NAME")) {
				System.out.println("Please enter the new NAME: ");		
				Scanner s2 = new Scanner(System.in);
				newName = s2.nextLine();		
				queryText2 =
					"UPDATE yrb_customer"
				+ "    SET name = ?    "
				+ "    WHERE cid = ?   ";
			}
			else if (update_item.equals("CITY"))
			{
				System.out.println("Please enter the new CITY: ");		
				Scanner s3 = new Scanner(System.in);
				newCity = s3.nextLine();				
				queryText2 =
					"UPDATE yrb_customer"
				+ "    SET city = ?    "
				+ "    WHERE cid = ?   ";
			}			
			else 
			{
				System.out.println("**Please enter NAME or CITY**");
			}
		}
        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText2);
        } catch(SQLException e) {
            System.out.println("update_customer failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
			if (update_item.equals("NAME")) {

				querySt.setString(1, newName);
				querySt.setInt(2, custID.intValue());
				answers = querySt.executeUpdate();
			}else{
				querySt.setString(1, newCity);
				querySt.setInt(2, custID.intValue());
				answers = querySt.executeUpdate();
			}
	
        } catch(SQLException e) {
            System.out.println("update_customer failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }
		

	 }
	
	/**
	* fetch_categories
	**/
	
	public ArrayList<String>  fetch_categories() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.
		ArrayList<String> al = new ArrayList<String>();
		
        queryText =
            "SELECT   cat"
          + "    FROM yrb_category";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("fetch_categories failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("fetch_categories failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }


        // Walk through the results and present them.
        try {
			ResultSetMetaData rsmd = answers.getMetaData();
			int columnsNumber = rsmd.getColumnCount();			
			System.out.println("");			
            System.out.println("Categories that we offer:");
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~");
			while (answers.next()) {
				for (int i = 1; i <= columnsNumber; i++) {
					if (i > 1) System.out.print(",  ");
					String columnValue = answers.getString(i);
					System.out.print(columnValue);
					al.add(columnValue);
				}
			System.out.println("");
			}
			
        } catch(SQLException e) {
            System.out.println("fetch_categories failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("fetch_categories failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("fetch_categories failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
		return al;
	}
	 
	 
	 /**
	 * find_book
	 **/
    public void find_book() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        queryText =
            "SELECT  title, year, weight, language"
          + "    FROM yrb_book B         "
          + "    WHERE B.cat = ? AND B.title = ?";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("find_book failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
				querySt.setString(1, category);
				querySt.setString(2, book);
				answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("find_book failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Variables to hold the column value(s).
		

		int count = 1;
        // Walk through the results and present them.
        try {
			if (answers.next()) {
    			title = answers.getString("title");
				year = answers.getString("year");
				weight = answers.getString("weight");
				language = answers.getString("language");		
 			System.out.println("");
			System.out.println("Book information: ");
			System.out.println("----------------");
			System.out.println("Title: " + title);
			System.out.println("Year: " + year);
			System.out.println("Language: " + language);	
			System.out.println("Weight: " + weight);		
			count++;
			bookYear = year;
			
			
				//If more than 1 title returned, have user select which edition year they want
				if(count > 2) {
				System.out.println("Please enter the YEAR of the desired title: ");
				Scanner s2 = new Scanner(System.in);
				bookYear = s2.nextLine();
				}
            } 
			else{
				System.out.println("");	
				System.out.println("I couldn't find that book. Please try again.");
				find_book();
			}

        } catch(SQLException e) {
            System.out.println("find_book failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }
		

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("find_book failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("find_book failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

	}	 
	/**
	 * display_books
	 **/

	public ArrayList<String> display_books() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.
		ArrayList<String> al = new ArrayList<String>();
		
        queryText =
            "SELECT   title    "
          + "    FROM yrb_book B"
		  + "    WHERE B.cat = ?";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("fetch_categories failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
			querySt.setString(1, category);
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("fetch_categories failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Variables to hold the column value(s).

        // Walk through the results and present them.
        try {
			ResultSetMetaData rsmd = answers.getMetaData();
			int columnsNumber = rsmd.getColumnCount();			
			System.out.println("");			
            System.out.println("List of books in " + category + ":");
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
			while (answers.next()) {
				for (int i = 1; i <= columnsNumber; i++) {
					if (i > 1) System.out.print(",  ");
					String columnValue = answers.getString(i);
					System.out.print(columnValue);
					al.add(columnValue);
				}
			System.out.println("");
			}
				System.out.println("");
        } catch(SQLException e) {
            System.out.println("fetch_categories failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("fetch_categories failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("fetch_categories failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
		return al;
	}
	 
	 
	
	/**
	* min_price
	**/
	
  public void min_price() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.
		float price = 0;
		float total = 0;
		boolean check = false;
        queryText =
            "SELECT   min(O.price), O.club         "
          + "    FROM yrb_offer O, yrb_member M, yrb_book B         "
          + "    WHERE O.club = M.club and O.title = B.title and O.year = B.year and M.cid = ? and O.title = ? and O.year = ? and B.cat = ?"
		  + "    GROUP BY O.club"
		  + "    ORDER BY  min(O.price)";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("min_price failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setInt(1, custID.intValue());
			querySt.setString(2, title);
			querySt.setString(3, bookYear);
			querySt.setString(4, category);
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("min_price failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Variables to hold the column value(s).
		DecimalFormat df = new DecimalFormat("####0.00");

        // Walk through the results and present them.
        try {
			while (answers.next()) {
                price = answers.getFloat("1");   
					offer = answers.getString("club");
            } 
			System.out.println("");
			System.out.println("The minimum price for " + title + " from the " + category + " category is $" + price);	
			System.out.println("");
			System.out.println("Enter the number of book(s) you would like to buy:");	
			
			int quantity = -1;
			while(!check)
			{
				Scanner s = new Scanner(System.in);
				quantity = s.nextInt();
				if (quantity >= 0)
				{
					check = true;
					total = quantity * (float) price;
				}
				else{
					System.out.println("Please enter a non-negative number");		
				}			
				
				
			}
			System.out.println("");
			System.out.println("Your total cost is $" + df.format(total));
			System.out.println("");
			System.out.println("Would you like to make this purchase? [Y/N]");			
			Scanner s1 = new Scanner(System.in);				
			String response = s1.nextLine();
			if (response.equals("Y")){
				insert_purchase(quantity);
				System.out.println("Thank you for your purchase. The transcation has been recorded.");
			}
			else{
				System.out.println("\nHave a great day.");
			}
			
        } catch(SQLException e) {
            System.out.println("min_price failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("min_price failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("min_price failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
	}	 
	 
	 /******************** 
	 *	insert_purchase   
	 *********************/
	 
	public void insert_purchase(int purchaseAmount) {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        int        answers   = 0;   // A cursor.

        queryText =
            	"INSERT INTO yrb_purchase (cid, club, title, year, when, qnty)"
			+ "    VALUES (?, ?, ?, ?, ?, ?)";
			

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("insert_purchase failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }
		//For getting the current time
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
		Calendar now = Calendar.getInstance();
		
        // Execute the query.
        try {
            querySt.setInt(1, custID.intValue());
			querySt.setString(2, offer);
			querySt.setString(3, title);
			querySt.setString(4, bookYear);
			querySt.setString(5, sdf.format(now.getTime()));
			querySt.setInt(6, purchaseAmount);
            answers = querySt.executeUpdate();
        } catch(SQLException e) {
            System.out.println("insert_purchase failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("insert_purchase failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
	}
	 
    public static void main(String[] args) {
        CustTotal ct = new CustTotal(args);
    }
}
	