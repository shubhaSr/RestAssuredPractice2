package TravianGames.TravianGamesTask;

import static io.restassured.RestAssured.given;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class TestInvalidTodoSchemaItems {
	
	@BeforeClass
	public void beforeClass() {
		RequestSpecBuilder requestSpecBuider = new RequestSpecBuilder();
		requestSpecBuider.setBaseUri("https://shuba-qa-interview-todo-api-axge4.ondigitalocean.app/");
		RestAssured.requestSpecification = requestSpecBuider.build();
		
		ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder().
				expectContentType(ContentType.JSON).
				log(LogDetail.ALL);
		RestAssured.responseSpecification = responseSpecBuilder.build();
	}
	
	@DataProvider(name = "invalid_schema_values")
	public Object[][] invalid_schema_values() {
		return new Object[][] {
			{"{\"name\":\"shubha\", \"position\": \"QA\"}", "{\"text\": \"abc\"}"},
			{"{\"text\": \"abc\"}", "{\"text\": false}"},
			{"{\"text\": false}", "{\"text\": 1}"},
			{"{\"text\": 1}", "{\"done\": \"abc\"}"},
			{"{\"done\": \"abc\"}", "{\"done\": false}"},
			{"{\"done\": false}", "{\"done\": 1}"},
			{"{\"done\": 1}", "{}"},
			{"{}", "{\"name\":\"shubha\", \"position\": \"QA\"}"}
		};
	}
	
	@Test(dataProvider="invalid_schema_values")
	public void test_invalid_schema_values(String invalid_schema_post, String invalid_schema_put) {
		
		//Perform POST
		int id = given().
					contentType(ContentType.JSON).
					body(invalid_schema_post).
				 when().
					post("/todos").
				    then().
					statusCode(201).
					extract().path("id");
		
		
		//Perform PUT
		       given().
			       contentType(ContentType.JSON).
			       body(invalid_schema_put).
			       pathParams("id", id).
		      when().
			       put("/todos/{id}").
		      then().
			      statusCode(200).
			      extract().
			      response();
		
		//Perform Delete to remove it
Response response = given().
					     pathParam("id", id).
					when().
						 delete("/todos/{id}").
					then().
						 statusCode(200).
						 extract().
						response();
		Assert.assertEquals(response.getBody().asString(), "{}");
	}
}
