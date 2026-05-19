package com.keeply.expiry.entity;

import com.keeply.common.entity.BaseTimeEntity;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "expiry_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ExpiryItem extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns({
    @JoinColumn(name = "group_id", referencedColumnName = "group_id", nullable = false),
    @JoinColumn(name = "author_user_id", referencedColumnName = "user_id", nullable = false)
  })
  private GroupMember authorMember;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", insertable = false, updatable = false)
  private Group group;

  @Column(name = "product_name", nullable = false)
  @NotBlank
  private String productName;

  @Column(name = "expire_date", nullable = false)
  @NotNull
  private LocalDate expireDate;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;
}
