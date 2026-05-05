# Notification Service - Task Management System

Production-ready notification service using RabbitMQ for asynchronous email notifications.

## Features

- **Asynchronous Processing**: RabbitMQ message queue for non-blocking notifications
- **Email Notifications**: SMTP-based email delivery
- **Dead Letter Queue**: Failed messages automatically routed to DLQ for retry
- **Persistent Storage**: All notifications logged in MySQL database
- **JWT Security**: Secured endpoints with token-based authentication
- **RESTful API**: Query notification history and unread counts

## Architecture

```
Other Services → RabbitMQ → NotificationConsumer → EmailService → SMTP Server
                     ↓
                   Database (audit trail)
```

## Notification Events

### Supported Event Types

1. **CARD_ASSIGNED** - User assigned to a card
2. **CARD_DUE_DATE_APPROACHING** - Card due date reminder
3. **COMMENT_ADDED** - New comment on a card
4. **WORKSPACE_MEMBER_ADDED** - User added to workspace

## Setup

### 1. Start RabbitMQ

```bash
cd notification-service
docker-compose up -d
```

Access RabbitMQ Management UI: http://localhost:15672
- Username: `guest`
- Password: `guest`

### 2. Configure Environment Variables

Create `.env` or set in `application.properties`:

```properties
# Database
DB_USERNAME=root
DB_PASSWORD=root123

# JWT (must match auth-service)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Email (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 3. Gmail App Password Setup (if using Gmail)

1. Enable 2-factor authentication in Google Account
2. Go to: https://myaccount.google.com/apppasswords
3. Generate app password for "Mail"
4. Use generated password in `MAIL_PASSWORD`

### 4. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

Service runs on: http://localhost:8086

## API Endpoints

### Get User Notifications
```http
GET /api/notifications?page=0&size=20
Authorization: Bearer <jwt-token>
```

### Get Unread Count
```http
GET /api/notifications/unread-count
Authorization: Bearer <jwt-token>
```

Response:
```json
{
  "unreadCount": 5
}
```

## Integration with Other Services

### Option 1: Shared Library (Recommended)

Copy `NotificationEvent.java` and `NotificationPublisher.java` to other services.

**In other services (e.g., card-service):**

Add RabbitMQ dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Configure RabbitMQ:
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

Create RabbitMQ config (same as notification-service):
```java
@Configuration
public class RabbitMQConfig {
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";
    
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
```

Copy DTO:
```java
// Copy NotificationEvent.java to your service's dto package
```

Usage in service:
```java
@Service
@RequiredArgsConstructor
public class CardService {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void assignCard(Long cardId, String assigneeEmail) {
        // ... assign card logic
        
        NotificationEvent event = NotificationEvent.builder()
            .eventType("CARD_ASSIGNED")
            .recipientEmail(assigneeEmail)
            .recipientName("John Doe")
            .subject("You've been assigned to a card")
            .message("Check out your new task!")
            .entityId(cardId)
            .entityType("CARD")
            .workspaceId("workspace-123")
            .triggeredBy("manager@example.com")
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.NOTIFICATION_EXCHANGE,
            RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
            event
        );
    }
}
```

### Option 2: REST API (Not recommended for production)

```java
// Send via HTTP POST (creates coupling and latency)
restTemplate.postForEntity(
    "http://notification-service/api/notifications/send",
    notificationEvent,
    Void.class
);
```

## RabbitMQ Queue Configuration

- **Main Queue**: `notification.queue`
- **Exchange**: `notification.exchange` (Topic)
- **Routing Key**: `notification.routing.key`
- **Dead Letter Queue**: `notification.dlq`
- **DLQ Exchange**: `notification.dlq.exchange`

## Monitoring

### RabbitMQ Management UI
- URL: http://localhost:15672
- View queues, message rates, consumers

### Database Monitoring
```sql
-- Check notification stats
SELECT status, COUNT(*) as count 
FROM notifications 
GROUP BY status;

-- Failed notifications
SELECT * FROM notifications 
WHERE status = 'FAILED' 
ORDER BY created_at DESC;
```

## Testing

### Manual Test via RabbitMQ UI

1. Go to http://localhost:15672
2. Navigate to **Queues** → `notification.queue`
3. Click **Publish message**
4. Set payload:
```json
{
  "eventType": "CARD_ASSIGNED",
  "recipientEmail": "test@example.com",
  "recipientName": "Test User",
  "subject": "Test Notification",
  "message": "This is a test",
  "entityId": 1,
  "entityType": "CARD",
  "workspaceId": "test-workspace",
  "triggeredBy": "admin@example.com"
}
```
5. Click **Publish message**
6. Check logs and database

## Troubleshooting

### Email not sending
- Verify SMTP credentials
- Check firewall/antivirus blocking port 587
- Enable "Less secure app access" for Gmail (or use app password)
- Check logs: `tail -f logs/notification-service.log`

### Messages stuck in queue
- Check consumer is running: RabbitMQ UI → Connections
- Verify database connection
- Check for exceptions in logs

### Dead Letter Queue growing
- Review failed messages in DLQ
- Check error_message column in database
- Fix root cause and re-process manually

## Performance Tuning

### Concurrent Consumers
```properties
spring.rabbitmq.listener.simple.concurrency=5
spring.rabbitmq.listener.simple.max-concurrency=10
```

### Batch Processing
```java
@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
public void consumeBatch(List<NotificationEvent> events) {
    events.forEach(notificationService::processNotification);
}
```

## Security

- JWT validation on all endpoints
- Email templates sanitized to prevent XSS
- Rate limiting (implement if needed)
- PII data encrypted at rest (implement if needed)

## Production Checklist

- [ ] Configure external SMTP service (SendGrid, AWS SES)
- [ ] Set up SSL/TLS for RabbitMQ
- [ ] Enable RabbitMQ clustering for HA
- [ ] Configure log aggregation (ELK, Splunk)
- [ ] Set up monitoring alerts (queue depth, failure rate)
- [ ] Implement retry policy for failed emails
- [ ] Add rate limiting per user
- [ ] Regular cleanup of old notifications (>90 days)
