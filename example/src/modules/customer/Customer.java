package modules.customer;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import modules.salesperson.Salesperson;
import modules.transaction.Transaction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A Model.
 */
@Entity
@Table(name = "customer")
@GenericGenerator(name="jpa-uuid", strategy = "uuid")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Customer  implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "jpa-uuid")
    private String id;

    //客户姓名
    @Column(name = "name")
    private String name;
    //客户邮箱
    @Column(name = "email")
    private String email;
    //客户电话
    @Column(name = "phone")
    private String phone;
    //总消费金额
    @Column(name = "total_spent")
    private Integer totalSpent;
    //关联销售员
    @ManyToOne
    @JoinColumn(name = "salesperson_id")
    private Salesperson salespersonId;

    @OneToMany(mappedBy="customerId", targetEntity = Transaction.class)
    private List<Transaction> customerIdTransaction = new ArrayList<Transaction>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Transaction> getCustomerIdTransaction() {
        return customerIdTransaction;
    }

    public void setCustomerIdTransaction(List<Transaction> customerIdTransaction) {
        this.customerIdTransaction = customerIdTransaction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public Integer getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Integer totalSpent) {
        this.totalSpent = totalSpent;
    }
    public Salesperson getSalespersonId() {
        return salespersonId;
    }

    public void setSalespersonId(Salesperson salespersonId) {
        this.salespersonId = salespersonId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer)) {
            return false;
        }
        return id != null && id.equals(((Customer) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Customer{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", email='" + getEmail() + "'" +
            ", phone='" + getPhone() + "'" +
            ", totalSpent='" + getTotalSpent() + "'" +
            "}";
    }
}
