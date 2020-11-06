package AddressBookDB;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddressBookDBService {
	
	private PreparedStatement addressBookDataStatement;
	
	private Connection getConnection() throws SQLException {
		String jdbcURL = "jdbc:mysql://localhost:3306/AddressBookService?useSSL=false";
		String userName = "root";
		String password = "Admin@123";
		Connection connection;
		System.out.println("Connecting to database: " + jdbcURL);
		connection = DriverManager.getConnection(jdbcURL, userName, password); // used DriverManager to get the connection
		return connection;
	}

	public ArrayList<AddressBook> getDetails() throws AddressBookException {
		ArrayList<AddressBook> addressList = new ArrayList<>();
		try (Connection connection = this.getConnection()) {
			Statement statement = connection.createStatement();
			// retrieving from person table
			String sql = "select * from person;";
			ResultSet resultSet = statement.executeQuery(sql);
			addressList = getDetails(resultSet);	
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
		return addressList;	
	}
	
	public ArrayList<AddressBook> getDetails(ResultSet resultSet) throws SQLException {
		String firstName = "", lastName = "", email = "", state = "", city = "", zip = "", address = "";
		long phone = 0;
		ArrayList<AddressBook> addressList = new ArrayList<>();
		Connection connection = getConnection();
		while (resultSet.next()) {
			int id = resultSet.getInt(1);
			firstName = resultSet.getString(3);
			lastName = resultSet.getString(4);
			phone = resultSet.getLong(5);
			email = resultSet.getString(6);
			
			// retrieving from adress table
			Statement statement1 = connection.createStatement();
			String sql1 = String.format("select * from adress where person_id = %s;", id);
			ResultSet resultSet1 = statement1.executeQuery(sql1);
			while (resultSet1.next()) {
				state = resultSet1.getString(2);
				city = resultSet1.getString(3);
				zip = resultSet1.getString(4);
				address = resultSet1.getString(3) + resultSet1.getString(2);
			}
			addressList.add(new AddressBook(firstName, lastName, address, city, state, Integer.parseInt(zip), phone, email));
		}
		return addressList;
	}

	public int updateData(String name, String email) throws AddressBookException {
		String sql = "update person set email = ? where name = ? ;";
		try {
			Connection connection = getConnection();
			addressBookDataStatement = connection.prepareStatement(sql);
			addressBookDataStatement.setString(1, email);
			addressBookDataStatement.setString(2, name);
			return addressBookDataStatement.executeUpdate();
		} catch (SQLException e) {
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
	}

	public List<AddressBook> getPerson(String name) throws AddressBookException {
		List<AddressBook> list = new ArrayList<>();
		String sql = "Select * from person where name = ?;";
		try {
			Connection connection = getConnection();
			addressBookDataStatement = connection.prepareStatement(sql);
			addressBookDataStatement.setString(1, name);
			ResultSet resultSet = addressBookDataStatement.executeQuery();
			list = this.getDetails(resultSet);
			
		} catch (SQLException e) {
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
		return list;
	}
	
	public List<String> getBook(int bookId) throws AddressBookException {
		List<String> list = new ArrayList<>();
		String sql = "Select book_name from book where book_id = ?;";
		try {
			Connection connection = getConnection();
			addressBookDataStatement = connection.prepareStatement(sql);
			addressBookDataStatement.setInt(1, bookId);
			ResultSet resultSet = addressBookDataStatement.executeQuery();
			while(resultSet.next()) {
				list.add(resultSet.getString(1));
			}
		} catch (SQLException e) {
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
		return list;
	}

	public AddressBook addPersonWithDetails(int bookId, String bookName, String bookType, String firstName, String lastName, long phone, String email, String state, String city, String zip) throws AddressBookException {
		int personId = -1;
		Connection connection = null;
		AddressBook contact = null;
		
		// establishing connection
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
		}
		catch (SQLException e) {
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
		
		// inserting to first table
		try (Statement statement = connection.createStatement()) {
			String sql = "insert into person (book_id, name, phone, email) values (?, ?, ?, ?);";
			addressBookDataStatement = connection.prepareStatement(sql, addressBookDataStatement.RETURN_GENERATED_KEYS);
			addressBookDataStatement.setInt(1, bookId);
			addressBookDataStatement.setString(2, firstName);
			addressBookDataStatement.setLong(3, phone);
			addressBookDataStatement.setString(4, email);
			int rowAffected = addressBookDataStatement.executeUpdate();
			if (rowAffected == 1) {
				ResultSet resultSet = addressBookDataStatement.getGeneratedKeys();
				if (resultSet.next())
					personId = resultSet.getInt(1);
			} 
		} catch (SQLException e) {
			try {
				e.printStackTrace();
				connection.rollback();
				return contact;
			} catch (SQLException e1) {
				e.printStackTrace();
				throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
			}
		}
		
		// inserting into 2nd table
		try (Statement statement = connection.createStatement()) {
			List<String> book = new ArrayList<>();
			book = this.getBook(bookId);
			if(book.isEmpty()) {
				String sql = "insert into book (book_id, book_name, type) values (?, ?, ?);";
				addressBookDataStatement = connection.prepareStatement(sql);
				addressBookDataStatement.setInt(1, bookId);
				addressBookDataStatement.setString(2, bookName);
				addressBookDataStatement.setString(3, bookType);
				int rowAffected = addressBookDataStatement.executeUpdate();
				if (rowAffected == 0) {
					return contact;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e.printStackTrace();
				throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
			}
		}
		
		// inserting into third table
		try (Statement statement = connection.createStatement()) {
			String sql = String.format("insert into adress (person_id, state, city, zip) values (%s, '%s', '%s', '%s')", personId, city, state, zip);
			int rowAffected = statement.executeUpdate(sql);
			if (rowAffected == 1) {
				contact = new AddressBook(firstName, lastName , city, city, state, Integer.parseInt(zip), phone, email);
			}
		} catch (SQLException e) { 
			e.printStackTrace();
			try {
				connection.rollback();
				return contact;
			} catch (SQLException e1) {
				e.printStackTrace();
				throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
			}
		}
		
		// for final committing
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
		
		// closing the connection
		finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
					throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
				}
			}
		}
		return contact;
	}

	public List<String> getPersonByCity(String city) throws AddressBookException {
		List<String> list = new ArrayList<>();
		String sql = "select p.name from person p natural join adress a where a.city = ? ;";
		try {
			Connection connection = getConnection();
			addressBookDataStatement = connection.prepareStatement(sql);
			addressBookDataStatement.setString(1, city);
			ResultSet resultSet = addressBookDataStatement.executeQuery();
			while (resultSet.next()) {
				list.add(resultSet.getString(1));
			}
		} catch (SQLException e) {
			throw new AddressBookException(e.getMessage(), AddressBookException.ExceptionType.SQL_EXCEPTION);
		}
		return list; 
	}
}
