package TravianGames.TravianGamesTask;


import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class TestTodoRequests {
	/* Array to keep track of ids of new todos added to server. 
	 * Used in GET, PUT and DELETE request for getting valid id
	 * and in afterClass to delete all element to bring server to initial state. 
	 */
	ArrayList<Integer> list_of_id;
	
	@BeforeClass
	public void beforeClass() {
		RequestSpecBuilder requestSpecBuider = new RequestSpecBuilder();
		requestSpecBuider.setBaseUri("https://shuba-qa-interview-todo-api-axge4.ondigitalocean.app/");
		RestAssured.requestSpecification = requestSpecBuider.build();
		
		ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder().
				expectContentType(ContentType.JSON).
				log(LogDetail.ALL);
		RestAssured.responseSpecification = responseSpecBuilder.build();
		list_of_id = new ArrayList<Integer>();
	}
	
	@AfterClass
	public void afterClass() {
		this.delete_added_todos();
	}
	
	public void delete_added_todos() {
		for (Integer id_to_delete : list_of_id) {
			given().
				pathParam("id", id_to_delete).
			when().
				delete("/todos/{id}");
		}
	}
	
	
	
	// Data Provider for DELETE and GET request
	@DataProvider(name = "different_ids_to_test")
	public Object[][] different_ids_to_test() {
		Random random_method = new Random();
		int index = random_method.nextInt(list_of_id.size()); 
		int valid_id = list_of_id.get(index);
		// Assuming 3000 doesn't exist. Need optimization to ensure it
		int invalid_id = 3000;
		
		return new Object[][] {
			{valid_id, true, "existing id"},
			{invalid_id, false, "non existing id"}
		};
	}
	
	
	// Data provider for POST request
	@DataProvider(name = "todos_to_add")
	public Object[][] todos_to_add() {
		return new Object[][] {
			{"text 1", false, "todo item with false done status"},
			{"text 2", true, "todo item with true done status"},
			{"", false, "todo item with empty text"},
			{"$#3256-abc", true, "todo item with special chars"},
			{"ABCD ", true, "todo item with uppercase chars"},
			{"AbCd 123", true, "todo item with upper and lower case chars"}
		};
	}
	
	

	// Data provider for PUT request
	@DataProvider(name = "update_todo")
	public Object[][] update_todo() {
		Random random_method = new Random();
		int index = random_method.nextInt(list_of_id.size()); 
		int id = list_of_id.get(index);
		return new Object[][] {
			{id, "text update", true, "update text content"},
			{id, "text update", false, "update done content"},
			{id, "update text", true, "update text and done content"}
		};
	}

	// POST request
	@Test(dataProvider = "todos_to_add",description = "This test does post request with multiple set of data")
	public void test_post_a_new_todo(String text_to_add, boolean done_to_add, String description) {
		JSONObject json_object = new JSONObject();
		json_object.put("text", text_to_add);
		json_object.put("done", done_to_add);
		
		Response response = given().
								contentType(ContentType.JSON).
								body(json_object.toString()).
							when().
								post("/todos").
							then().
								body(matchesJsonSchemaInClasspath("todo-schema.json")).
			                    body("text", equalTo(text_to_add)).
			                    body("done", equalTo(done_to_add)).
								statusCode(201).
								extract().
								response();
		
		//Todo actual_todo_item = response.getBody().as(Todo.class);
		int id = response.getBody().jsonPath().getInt("id");
		list_of_id.add(id);
	}
	

	// PUT request
	@Test(dependsOnMethods ="test_post_a_new_todo", dataProvider = "update_todo",description = "this test updates the data through put request",groups = "positive")
	public void test_put_request_to_update(int id, String text_to_update, boolean done_to_update, String description) {
		JSONObject json_object = new JSONObject();
		json_object.put("text", text_to_update);
		json_object.put("done", done_to_update);
		
		given().
			contentType(ContentType.JSON).
			body(json_object.toString()).
			pathParams("id", id).
		when().
			put("/todos/{id}").
		then().
			body(matchesJsonSchemaInClasspath("todo-schema.json")).
			statusCode(200).
			body("id", equalTo(id)).
			body("text", equalTo(text_to_update)).
		    body("done", equalTo(done_to_update));
	}
	
	@Test(dependsOnMethods ="test_post_a_new_todo",description = "this test tries updates the data for invalid id",groups = "negative")
	public void test_put_request_for_invalid_id() {
		JSONObject json_object = new JSONObject();
		json_object.put("text", "invalid");
		json_object.put("done", false);
		
		given().
			contentType(ContentType.JSON).
			body(json_object.toString()).
			pathParams("id", "invalid").
		when().
			put("/todos/{id}").
		then().
			statusCode(404);
	}
	
	// DELETE request
	@Test(dependsOnMethods ="test_post_a_new_todo", dataProvider = "different_ids_to_test",description = "this test tries to delete valid and non existing id")
	public void test_delete_non_existing_todo_endpoint(int id, boolean is_id_exist, String description) {
		int http_status = 404;
		if (is_id_exist)
			http_status = 200;
		Response response = given().
								pathParam("id", id).
							when().
								delete("/todos/{id}").
							then().
								statusCode(http_status).
								extract().
								response();
		Assert.assertEquals(response.getBody().asString(), "{}");
		
		list_of_id.remove(new Integer(id));
	}

	//GET requests 
	@Test(dependsOnMethods ="test_post_a_new_todo",description = "The test performs get request")
	public void test_get_request() {
		get("/todos").
		then().
			body(matchesJsonSchemaInClasspath("todo-list-schema.json")).
			statusCode(200).
			log().ifError();
	}

	
	@Test(dependsOnMethods ="test_post_a_new_todo",description = "Perform get request using ids in pathParam",groups = "positive")
	public void test_get_request_with_path_parameter() {
		Random random_method = new Random();
		int index = random_method.nextInt(list_of_id.size());
		int id_to_get = list_of_id.get(index);
		
		given().
			pathParam("id", id_to_get).
		when().
			get("/todos/{id}").
		then().
			statusCode(200).
			body(matchesJsonSchemaInClasspath("todo-schema.json")).
			body("id", equalTo(id_to_get));
	}


	@Test(dependsOnMethods ="test_post_a_new_todo",description = "Perform get request using invalid ids in pathParam",groups = "negative" )
	public void test_get_request_with_invalid_path_parameter() {
		String id = "invalid";
		Response response = given().
								pathParam("id", id).
							when().
								get("/todos/{id}").
							then().
								assertThat().
								statusCode(404).
								extract().
								response();
		
		Assert.assertEquals(response.getBody().asString(), "{}");
	}
	
	
	@Test(dependsOnMethods ="test_post_a_new_todo", dataProvider="different_ids_to_test",description = "Perform get request using query param")
	public void test_get_request_with_query_parameters(int id_to_get, boolean is_id_exist, String description) {
		Response response = given().
								queryParam("id", id_to_get).
							when().
								get("/todos").
							then().
								assertThat().
								statusCode(200).
								body(matchesJsonSchemaInClasspath("todo-list-schema.json")).
								extract().
								response();
		
		if (is_id_exist) {
			int actual_id = response.getBody().jsonPath().getInt("[0].id");
			Assert.assertEquals(actual_id, id_to_get);
		} else {
			Assert.assertEquals(response.getBody().asString(), "[]");
		}
	}
	
	
	@Test(dependsOnMethods ="test_post_a_new_todo",description = "Perform get request using invalid query param",groups = "negative")
	public void test_get_request_with_non_existent_key_in_query_parameters() {
		given().
			queryParam("invalid", "abc").
		when().
			get("/todos").
		then().
			statusCode(200).
			body(matchesJsonSchemaInClasspath("todo-list-schema.json"));
	}
}
