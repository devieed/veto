package org.veto.core.rdbms.bean;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.veto.core.VetoBean;

import java.util.Date;

@Table(name = "teams", indexes = {
        @Index(columnList = "cn_name", unique = true),
        @Index(columnList = "en_name", unique = true),
        @Index(columnList = "status"),
        @Index(columnList = "created_at")
})
@Entity
@Setter
@Getter
@ToString
public class Team implements VetoBean{
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private String cnName;

        private String enName;

        private String icon;

        private Date createdAt;
        // 所在国家
        private String country;

        private Boolean status;

        @PrePersist
        public void prePersist() {
            this.createdAt = new Date();
        }
}
