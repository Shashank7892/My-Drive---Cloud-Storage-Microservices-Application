package Mydrive.com.mydrive.DTOS;

import lombok.Data;

@Data
public class Passwordupdaterequest {

    private String oldpassword;

    private String newpassword;
}
