package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.util.Date;

@Entity
@Table(name = "realname_authentication", indexes = {
        @Index(columnList = "user_id", unique = true),
        @Index(columnList = "real_name"),
        @Index(columnList = "phone"),
        @Index(columnList = "id_number"),
        @Index(columnList = "real_name_authentication_status")
})
@Setter
@Getter
public class RealNameAuthentication implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String realName;

    private String phone;

    private String idNumber;

    private Date createdAt;

    private Date updatedAt;

    @Column(name = "real_name_authentication_status")
    private REAL_NAME_AUTHENTICATION_STATUS realNameAuthenticationStatus;

    public enum  REAL_NAME_AUTHENTICATION_STATUS {
        NOT_YET, // 从未
        PENDING,
        NOT_PASSED, // 未通过
        PASS // 已通过
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    @PrePersist
    public void preInsert() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
    }
}
