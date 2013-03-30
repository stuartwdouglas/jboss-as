package org.jboss.as.iiop.tm;

import org.jboss.as.iiop.IIOPMessages;
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import javax.transaction.Transaction;

/**
 * @author Stuart Douglas
 */
public class CurrentIncomingTransaction {

    private static org.omg.PortableInterceptor.Current piCurrent = null;
    private static int slotId;

    public static void setPiCurrent(Current piCurrent) {
        //TODO: security checks
        CurrentIncomingTransaction.piCurrent = piCurrent;
    }

    public static void setSlotId(int slotId) {
        CurrentIncomingTransaction.slotId = slotId;
    }

    /**
     * Returns the transaction associated with the transaction propagation
     * context that arrived in the current IIOP request.
     */
    public static Transaction getCurrentTransaction() {
        Transaction tx = null;
        if (piCurrent != null) {
            // A non-null piCurrent means that a TxServerInterceptor was
            // installed: check if there is a transaction propagation context
            try {
                Any any = piCurrent.get_slot(slotId);
                if (any.type().kind().value() != TCKind._tk_null) {
                    // Yes, there is a TPC: add the foreign transaction marker
                    tx = ForeignTransaction.INSTANCE;
                }
            } catch (InvalidSlot e) {
                throw IIOPMessages.MESSAGES.errorGettingSlotInTxInterceptor(e);
            }

        }
        return tx;
    }

}
