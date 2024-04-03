package gcfv2pubsub;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.cloudevents.CloudEvent;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.logging.Logger;

public class PubSubFunction implements CloudEventsFunction {
  private static final Logger logger = Logger.getLogger(PubSubFunction.class.getName());

  @Override
  public void accept(CloudEvent event) throws SQLException {
    // Get cloud event data as JSON string
    String cloudEventData = new String(event.getData().toBytes());

    // Decode JSON event data to the Pub/Sub MessagePublishedData type
    Gson gson = new Gson();
    MessagePublishedData data = gson.fromJson(cloudEventData, MessagePublishedData.class);

    // Check if data is not null and if it contains a message
    if (data != null && data.getMessage() != null) {
      // Get the message from the data
      Message message = data.getMessage();
      // Get the base64-encoded data from the message & decode it
      String encodedData = message.getData();
      String decodedData = new String(Base64.getDecoder().decode(encodedData));
      // Log the message
      logger.info("Pub/Sub message: " + decodedData);

      JsonObject jsonPayload = gson.fromJson(decodedData, JsonObject.class);

      // Assuming decodedData contains JSON with an 'email' field
      // Extract the email address from the JSON payload
      String email = jsonPayload.get("username").getAsString();

      String ID = jsonPayload.get("UserId").getAsString();
      // Generate the verification link with a 2-minute expiration
      String verificationLink = generateVerificationLink(email, ID);

      // Send the verification email using Mailgun
      try {
        sendVerificationEmail(email, verificationLink, ID);
      } catch (IOException e) {
        logger.severe("Error sending verification email: " + e.getMessage());
      }
    } else {
      logger.info("Invalid or null data received from Pub/Sub.");
    }
  }

  private String generateVerificationLink(String email, String ID) throws SQLException {

    LocalDateTime expirationTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    String formattedExpirationTime = expirationTime.format(formatter);

    // Print or log the formatted expiration time
    System.out.println("Formatted Expiration Time: " + formattedExpirationTime);

    // Include the UserId (token) and expiration time in the verification link
    // String verificationLink = "http://keerthanamikkili.me:8080/verify-email?token=" + ID;
    String verificationLink = "https://keerthanamikkili.me/verify-email?token=" + ID;
    System.out.println("Verification Link: " + verificationLink);

    saveToDatabase(email, formattedExpirationTime, ID);
    return URLEncoder.encode(verificationLink, StandardCharsets.UTF_8);

  }

  private void saveToDatabase(String email, String formattedExpirationTime, String id) {
    String dbUrl = "jdbc:mysql://" + System.getenv("SQL_HOST") + "/" + System.getenv("webappSQL_DATABASE");
    String dbUsername = System.getenv("SQL_USERNAME");
    String dbPassword = System.getenv("SQL_PASSWORD");
    logger.info("dbUrl: " + dbUrl);
    logger.info("dbUsername: " + dbUsername);
    logger.info("dbPassword: " + dbPassword);
    try {
      // Connect to the database
      Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
      logger.info("Connected to the database.");

      // Prepare a SQL statement for insertion
      String insertSql = "INSERT INTO email_tracking (id, verification_expiration, user_name) VALUES (?, ?, ?)";

      // Create a PreparedStatement for executing the insertion
      PreparedStatement preparedStatement = conn.prepareStatement(insertSql);

      // Set the values for the placeholders in the prepared statement
      preparedStatement.setString(1, id); // Example ID value
      preparedStatement.setString(2, formattedExpirationTime); // Example verification_expiration value
      preparedStatement.setString(3, email); // Example user_name value

      // Execute the insertion
      int rowsAffected = preparedStatement.executeUpdate();
      logger.info("Rows affected by insertion: " + rowsAffected);

      // Close the PreparedStatement and Connection
      preparedStatement.close();
      conn.close();

    } catch (SQLException e) {
      logger.info("Error inserting data into the database: " + e.getMessage());
    }

  }

  private void sendVerificationEmail(String recipientEmail, String verificationLink, String ID) throws IOException {

    final String apiKey = System.getenv("MAILGUN_apiKey");
    final String domain = System.getenv("MAILGUN_domain");

    HttpClient httpClient = HttpClients.createDefault();

    HttpPost httpPost = new HttpPost("https://api.mailgun.net/v3/" + domain + "/messages");
    String emailContent = "Click the link below to verify your email address:\n" + verificationLink
        + "\n\nThis link will expire in 2 minutes.";
    System.out.println("Email Content:" + emailContent);

    httpPost.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + apiKey).getBytes()));
    httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

    StringEntity entity = new StringEntity(
        "from=New User Verification Mail <noreply@" + domain + ">&" +
            "to=" + recipientEmail + "&" +
            "subject=Verify Your Email Address&" +
            "text=" + emailContent,
        ContentType.APPLICATION_FORM_URLENCODED);

    httpPost.setEntity(entity);
    HttpResponse response = httpClient.execute(httpPost);

    // Corrected logger info line
    if (response != null) {
      logger.info("Mailgun API response: " + response.getStatusLine().getStatusCode());
    } else {
      logger.info("Mailgun API response: Response is null.");
    }
  }
}
