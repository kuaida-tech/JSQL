package modules.transaction;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import modules.customer.Customer;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * A Model.
 */
@Entity
@Table(name = "transaction")
@GenericGenerator(name="jpa-uuid", strategy = "uuid")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Transaction  implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "jpa-uuid")
    private String id;

    //客户
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customerId;
    //交易金额
    @Column(name = "amount")
    private Integer amount;
    //交易日期
    @Column(name = "transaction_date")
    private Date transactionDate;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public Customer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Customer customerId) {
        this.customerId = customerId;
    }
    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaction)) {
            return false;
        }
        return id != null && id.equals(((Transaction) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Transaction{" +
            "id=" + getId() +
            ", amount='" + getAmount() + "'" +
            ", transactionDate='" + getTransactionDate() + "'" +
            "}";
    }
}
