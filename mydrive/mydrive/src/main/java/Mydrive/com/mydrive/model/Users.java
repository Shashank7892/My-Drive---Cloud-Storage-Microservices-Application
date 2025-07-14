package Mydrive.com.mydrive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "userdetails")
@AllArgsConstructor
@NoArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true,nullable = false)
    private String username;

    @Column(unique = true,nullable = false)
    private String email;

    @Column(name = "encoded_password",nullable = false)
    private String password;

    @Column(name="leftallocated_storage",nullable = false)
    private Long allallocatedstorageBytes; //free storage of 8G for all users

    @Column(name = "used_storage",nullable = false)
    private Long usedstorageBytes;

    @Column(nullable = false)
    private String role;  //user //admin

    @Column(name = "current_plan",nullable = false)
    private String currentPlan;

    @Column(nullable = false)
    private LocalDateTime createdtime;

    @Column(nullable = false)
    private LocalDateTime updatedtime;


    // this below code can also be done in service layer
    @PrePersist
    private void onCreate(){
        this.createdtime=LocalDateTime.now();
        this.updatedtime=LocalDateTime.now();
        if(this.allallocatedstorageBytes==null){
            this.allallocatedstorageBytes=8L*1024*1024*1024; //default 8GB storage for every new user
        }
        if(this.usedstorageBytes==null) {
            this.usedstorageBytes = 0L; //used memory of each user
        }
        if(this.role==null){
            this.role="USER";
        }
        if(this.currentPlan==null){
            this.currentPlan="Free";
        }
    }

    @PreUpdate
    private void onUpdate(){
        this.updatedtime=LocalDateTime.now();
    }

}

