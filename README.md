#Email forwarder
Scala AWS Lambda function build with Gradle and Serverless to forward email from an SNS message to a configured email address.

#Setup

* Install & setup Serverless
* Checkout this repository and configure your mail addresses in `config.properties` file. See the `config.properties.example' for an example.
* Build the code with gradle: `gradle build`
* Deploy the lambda function with `serverless deploy`. This will create an SNS topic.
* Configure AWS SES to receive e-mail for your own domain: http://docs.aws.amazon.com/ses/latest/DeveloperGuide/receiving-email.html. This might take upto 72 hours to take effect (though in my case it was in less than half an hour).
* Configure a receipt rule that pushes messages on the SNS topic created by Serverless.
* Serverless created a new IAM role for the Lambda function. The function will use SES to forward the mail, therefore add a new role to the policy that allows email sending:
    ```
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Stmt1474227740000",
                "Effect": "Allow",
                "Action": [
                    "ses:SendEmail",
                    "ses:SendRawEmail"
                ],
                "Resource": [
                    "*"
                ]
            }
        ]
    }
    ```

Now try to send an e-mail to an e-mail address on your domain and you should receive it on the configured 'to' address.
For troubleshooting use `serverless logs -f forwarder` to view the log files
