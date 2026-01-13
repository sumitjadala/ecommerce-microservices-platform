resource "aws_sns_topic" "order_events" {
  name = "order-events-topic"
}

resource "aws_sns_topic" "payment_events" {
  name = "payment-events-topic"
}
