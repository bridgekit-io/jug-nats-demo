package io.bridgekit.nats.sampleapp.payments;

import java.util.List;

import org.slf4j.LoggerFactory;

import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.STATUS_AUTHORIZED;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.STATUS_CHARGEBACK;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.STATUS_CHARGED;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.STATUS_REFUNDED;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.sampleapp.EventGateway.Publisher;

public class PaymentServiceHandler implements PaymentService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PaymentServiceHandler.class);
    private final Logger logger;
    private final Publisher eventPublisher;
    private final TransactionRepo transactionRepo;

    public PaymentServiceHandler(Publisher eventPublisher) {
        this.logger = Logger.instance(PaymentService.class);
        this.eventPublisher = eventPublisher;
        this.transactionRepo = new TransactionRepo();
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<Transaction> searchTransactions(SearchTransactionsCriteria criteria) {
        logger.info("Searching customer's transactions");
        return transactionRepo.search();
    }

    /**
     * @inheritDoc
     */
    @Override
    public Transaction getTransaction(GetTransactionRequest req) {
        logger.info("Fetching transaction: %s", req.transactionID);
        return transactionRepo.get(req.transactionID, "");
    }

    /**
     * @inheritDoc
     */
    @Override
    public Transaction authorize(AuthorizeRequest req) {
        logger.info("Authorizing payment of %d for order %s", req.total, req.orderID);

        var transaction = new Transaction();
        transaction.orderID = req.orderID;
        transaction.total = req.total;
        transaction.processorID = req.processorID;
        transaction.processorToken = req.processorToken;
        transaction.status = STATUS_AUTHORIZED;
        transaction = transactionRepo.create(transaction);

        eventPublisher.publish("payment.authorized", transaction);
        return transaction;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Transaction charge(ChargeRequest req) {
        var transaction = transactionRepo.get(req.transactionID, req.orderID);

        switch (transaction.status) {
        case STATUS_REFUNDED:
        case STATUS_CHARGEBACK:
            throw new IllegalStateException("Transaction already reversed: " + transaction);
        case STATUS_CHARGED:
            logger.info("Transaction already processed; ignoring charge: %s", transaction);
            return transaction;
        default:
            logger.info("Charging payment method: %s", transaction.status);
            transaction.status = STATUS_CHARGED;
            transaction = transactionRepo.update(transaction);
        }

        eventPublisher.publish("payment.charged", transaction);
        return transaction;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Transaction refund(RefundRequest req) {
        var transaction = transactionRepo.get(req.transactionID, req.orderID);

        switch (transaction.status) {
        case STATUS_CHARGEBACK:
        case STATUS_REFUNDED:
            logger.info("Transaction already reversed; ignoring refund: %s", transaction);
            return transaction;
        default:
            logger.info("Processing refund: %s", transaction);
            transaction.status = STATUS_REFUNDED;
            transaction = transactionRepo.update(transaction);
        }

        eventPublisher.publish("payment.refunded", transaction);
        return transaction;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Transaction chargeback(ChargebackRequest req) {
        var transaction = transactionRepo.get(req.transactionID, "");

        switch (transaction.status) {
        case STATUS_CHARGEBACK:
        case STATUS_REFUNDED:
            logger.info("Transaction already reversed; ignoring chargeback: %s", transaction);
            return transaction;
        default:
            logger.info("Processing chargeback: %s", transaction);
            transaction.status = STATUS_CHARGEBACK;
            transaction = transactionRepo.update(transaction);
        }

        eventPublisher.publish("payment.chargeback", transaction);
        return transaction;
    }
}
