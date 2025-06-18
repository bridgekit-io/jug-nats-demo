package io.bridgekit.nats.sampleapp.payments;

import java.util.List;

/**
 * PaymentService provides some very basic operations used for credit card processing. No, this
 * doesn't *really* process any credit cards. It just spits out some logging to show the new
 * states of the transactions it's interacting with.
 */
public interface PaymentService {
    /**
     * Finds all customer transactions based on the specified criteria.
     *
     * @param criteria The search criteria
     * @return A non-null list of the matching transactions.
     */
    List<Transaction> searchTransactions(SearchTransactionsCriteria criteria);

    /**
     * Fetches a single transaction by its id.
     */
    Transaction getTransaction(GetTransactionRequest req);

    /**
     * Places a hold on the customer's credit card for the specified amount. The charge will
     * come later once the underlying order ships.
     */
    Transaction authorize(AuthorizeRequest req);

    /**
     * Executes the authorization/hold on the transaction's payment method to actually
     * charge the credit card, making their money become my money.
     */
    Transaction charge(ChargeRequest req);

    /**
     * Our webhook with the transaction processor that is fired when the customer issues a
     * chargeback directly with their credit card company. They didn't explicitly cancel their
     * order with us. This updates the transaction record to reflect that we no longer have that
     * money.
     * <p>
     * Pay attention to the events set up in Main. When this happens, the OrderService should
     * be listening to ensure that the order associated with this transaction is cancelled.
     */
    Transaction chargeback(ChargebackRequest req);

    /**
     * Called when the user cancels an order through our portal. It reverses the transaction and
     * sadly turns our money back into their money, again ::sad face::
     */
    Transaction refund(RefundRequest req);

    /**
     * The DTO for the financial transaction associated with an order.
     */
    class Transaction {
        public static final String STATUS_AUTHORIZED = "AUTHORIZED";
        public static final String STATUS_CHARGED = "CHARGED";
        public static final String STATUS_CHARGEBACK = "CHARGEBACK";
        public static final String STATUS_REFUNDED = "REFUNDED";

        public static final String PROCESSOR_STRIPE = "STRIPE";
        public static final String PROCESSOR_APPLE_PAY = "APPLE_PAY";

        public String transactionID;
        public String orderID;
        public long total;
        public String status;
        public String processorID;
        public String processorToken;

        @Override
        public String toString() {
            return "Transaction[ID:" + transactionID + ", OrderID:" + orderID + ", Status:" + status + "]";
        }

        @Override
        public int hashCode() {
            return transactionID.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Transaction otherTransaction) {
                return transactionID.equals(otherTransaction.transactionID);
            }
            return false;
        }
    }

    class SearchTransactionsCriteria {
    }

    class GetTransactionRequest {
        public String transactionID;
    }

    class AuthorizeRequest {
        public String orderID;
        public long total;
        public String processorID;
        public String processorToken;
    }

    class ChargeRequest {
        public String transactionID;
        public String orderID;
    }

    class ChargebackRequest {
        public String transactionID;
    }

    class RefundRequest {
        public String transactionID;
        public String orderID;
    }
}
