package momo.demo.constant;

public class MomoConstant {
    public static final String REQUEST_EVENT_BUS = "request-event-bus";
    public static final String RESPONSE_EVENT_BUS = "response-event-bus";
    public static final String FANOUT_EXCHANGE_TYPE = "fanout";
    public static final String DIRECT_EXCHANGE_TYPE = "direct";
    public static final String TAXI_EXCHANGE = "taxi-exchange";
    public static final String REQUEST_QUEUE = "order-taxi-request";
    public static final String RESPONSE_QUEUE = "order-taxi-response";
    public static final String REQUEST_ROUTING_KEY = "order.taxi.request";
    public static final String RESPONSE_ROUTING_KEY = "order.taxi.response";
    public static final String RABBIT_URI = "amqp://momo:momo@localhost:5672/%2f";
    public static final String STUB_DATA_FROM_CONSUMER = "STUB_DATA_FROM_CONSUMER";
}
