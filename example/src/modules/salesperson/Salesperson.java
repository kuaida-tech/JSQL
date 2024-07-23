package modules.salesperson;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import modules.customer.Customer;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A Model.
 */
@Entity
@Table(name = "salesperson")
@GenericGenerator(name="jpa-uuid", strategy = "uuid")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Salesperson  implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "jpa-uuid")
    private String id;

    //销售员姓名
    @Column(name = "name")
    private String name;
    //销售员邮箱
    @Column(name = "email")
    private String email;
    //销售员电话
    @Column(name = "phone")
    private String phone;

    @OneToMany(mappedBy="salespersonId", targetEntity = Customer.class)
    private List<Customer> salespersonIdCustomer = new ArrayList<Customer>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Customer> getSalespersonIdCustomer() {
        return salespersonIdCustomer;
    }

    public void setSalespersonIdCustomer(List<Customer> salespersonIdCustomer) {
        this.salespersonIdCustomer = salespersonIdCustomer;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Salesperson)) {
            return false;
        }
        return id != null && id.equals(((Salesperson) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Salesperson{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", email='" + getEmail() + "'" +
            ", phone='" + getPhone() + "'" +
            "}";
    }
}
