# SERVERLESS CLOUD FUNCTION

# Implement Cloud Function
This repo outlines the steps to implement a Cloud Function that will be triggered by a Pub/Sub event when a new user account is created. The Cloud Function will perform the following tasks:

## Email Verification:
 When triggered, the Cloud Function will email the user a verification link that they can click to verify their email address. The verification link will expire after 2 minutes.  An expired link cannot be used to verify the user.

## Tracking Emails in CloudSQL: 
Additionally, the Cloud Function will track the emails sent in a CloudSQL instance. You can utilize the same instance and database used by the web application for simplicity and consistency.

### Prerequisites:
Before proceeding, ensure you have the following:
Access to Google Cloud Platform (GCP) Console with the necessary permissions to create and manage resources.
Basic knowledge of Google Cloud Pub/Sub, Cloud Functions, and CloudSQL.
Set up a Pub/Sub topic to trigger the Cloud Function when a new user account is created.
A CloudSQL instance and database configured for use by both the web application and the Cloud Function.

### Implementation Steps:
1. Create Pub/Sub Topic
Ensure you have a Pub/Sub topic configured to trigger the Cloud Function. If not, create one using the GCP Console or via Terraform.

1. Configure Cloud Function
Implement the Cloud Function with the following logic:

Listen for Pub/Sub events.
When triggered, generate a verification link with a 2-minute expiration.
Email the verification link to the user.
Track the sent emails in the CloudSQL instance.
2. Set Up CloudSQL Database
Ensure your CloudSQL instance is properly configured to track the emails sent by the Cloud Function. You can use the same instance and database schema used by the web application.

3. Deploy Cloud Function
Deploy the Cloud Function to Google Cloud Platform using the appropriate deployment method (GCP Console, gcloud CLI, or Terraform).

4. Test the Cloud Function
Test the Cloud Function by simulating a new user account creation event. Verify that the user receives the email with the verification link and that the sent emails are tracked in the CloudSQL instance.

### Additional Considerations:
1. Security: 
Ensure that sensitive information such as API keys, email content, and database credentials are handled securely within the Cloud Function.
2. Error Handling:
 Implement robust error handling mechanisms within the Cloud Function to handle failures gracefully and prevent data loss.
3. Monitoring and Logging:
 Set up monitoring and logging for the Cloud Function to track its performance and troubleshoot any issues that arise.
4. Scalability:
 Consider the scalability requirements of your application and design the Cloud Function to handle varying loads efficiently.
