package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "withdraw", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "amount"),
        @Index(columnList = "created_at"),
        @Index(columnList = "status"),
})
@Setter
@Getter
public class Withdraw implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private Date createdAt;

    private String address;

    private Boolean isBlockchain;

    private BigDecimal fee;

    @Lob
    private String description;

    @Enumerated(EnumType.ORDINAL)
    private STATUS status;

    public enum STATUS{
        PENDING,
        APPLYING,
        APPLY,
        CANCEL,
        FORBID;

        public static STATUS getStatus(String status){
            if (status == null){
                return null;
            }

            for (STATUS value : STATUS.values()) {
                if (value.name().equalsIgnoreCase(status)){
                    return value;
                }
            }

            return null;
        }
    }

    @PrePersist
    public  void prePersist() {
        this.createdAt = new Date();
    }
}
