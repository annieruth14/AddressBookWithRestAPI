package AddressBookDB;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.google.gson.Gson;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class AddressBookTest {
	
	// check number of entries in the person and adress table
	@Test 
	public void givenDatabase_shouldReturnCount() throws AddressBookException {
		ContactService contact = new ContactService();
		List<AddressBook> listOfAddress = contact.getListFromDB();
		Assert.assertEquals(10, listOfAddress.size());
	}
	
	// update data
	@Test
	public void givenNewAddress_whenUpdated_shouldSyncWithDatabase() throws AddressBookException {
		ContactService contact = new ContactService();
		List<AddressBook> listOfAddress = contact.getListFromDB();
		contact.updateContact("Brian", "brian@microsoft.com");
		boolean result = contact.checkInSync("Brian");
		Assert.assertTrue(result);
	}
	
	// add data to table and list
	@Test
	public void givenNewAddress_whenInsertingInMultipleTables_shouldInsertInTheDatabase() throws AddressBookException {
		ContactService contact = new ContactService();
		boolean result = contact.addPersonInDB(6,"book6","batchmates","Neeraj","Ghosh",9874569362L,"james@gmail.com","Tripura","Agartala","258796");
		Assert.assertTrue(result);
	}
	
	// retrieve data by city
	@Test
	public void givenCity_shouldReturnPersons() throws AddressBookException {
		ContactService contact = new ContactService();
		List<String> listOfPersons = contact.getPersonsFromCity("Bangalore");
		Assert.assertEquals(3, listOfPersons.size());
	}
	
	// add multiple contacts using threads
	@Test
	public void givenMultipleContacts_whenAdded_shouldBeAddedToDatabase() throws AddressBookException {
		AddressBook[] contacts = {
				new AddressBook("Sara", "Singh", "Bidhanpally", "Siliguri", "WB", 7894561, 9874563258L, "sara@gmail.com"),
				new AddressBook("Ashish", "Gopal", "Kadamtala", "Kolkata", "WB", 569856, 874596665L, "ashish@gmail.com"),
				new AddressBook("Alok", "Singh", "Kormangala", "Bangalore", "Karnataka", 6985554, 9874588774L, "alok@gmail.com")
		};
		ContactService service = new ContactService();
		Instant start = Instant.now();
		service.addPersonInDB(Arrays.asList(contacts), 9, "book9", "roommates");
		Instant end =Instant.now();
		System.out.println("Duration with thread: "+ Duration.between(start, end));
		List<AddressBook> listOfAddress = service.getListFromDB();
		Assert.assertEquals(13, listOfAddress.size());
	}
	
	// REST and HAMCREST
	@Before
	public void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = 3000;
	}
	
	private AddressBook[] getContactListFromJsonServer() {
		Response response = RestAssured.get("/contacts");
		System.out.println("AddressBook Entries in JSON Server: \n" + response.asString());
		AddressBook[] arrayOfContacts = new Gson().fromJson(response.asString(), AddressBook[].class);
		return arrayOfContacts;
	}

	private Response addContactToJsonServer(AddressBook contact) {
		String contactJson = new Gson().toJson(contact);
		RequestSpecification request = RestAssured.given();
		request.header("Content-Type", "application/json");
		request.body(contactJson);
		return request.post("/contacts");
	}
	
	@Test
	public void givenContactDataInJsonServer_whenRetrieved_shouldMatchTheCount() {
		AddressBook[] arrayOfPerson = getContactListFromJsonServer();
		ContactService contactService = new ContactService();
		contactService.addToAddressList(Arrays.asList(arrayOfPerson));
		int entries = contactService.countEntries();
		Assert.assertEquals(5, entries);
	}
	
	@Test
	public void givenMultipleContacts_whenAdded_shouldMatchResponse200() {
		AddressBook[] arrayOfPerson = getContactListFromJsonServer();
		ContactService contactService = new ContactService();
		contactService.addToAddressList(Arrays.asList(arrayOfPerson));
		AddressBook[] contacts = {
				new AddressBook("Sara", "Singh", "Bidhanpally", "Siliguri", "WB", 7894561, 9874563258L, "sara@gmail.com"),
				new AddressBook("Ashish", "Gopal", "Kadamtala", "Kolkata", "WB", 569856, 874596665L, "ashish@gmail.com"),
				new AddressBook("Alok", "Singh", "Kormangala", "Bangalore", "Karnataka", 6985554, 9874588774L, "alok@gmail.com")
		};
		for (AddressBook contact : contacts) {
			Response response = addContactToJsonServer(contact);
			int statusCode = response.getStatusCode();
			Assert.assertEquals(201, statusCode);
			contact = new Gson().fromJson(response.asString(), AddressBook.class);
			contactService.addPersonInList(contact);
		}
		int entries = contactService.countEntries();
		Assert.assertEquals(8, entries);
	}
}
