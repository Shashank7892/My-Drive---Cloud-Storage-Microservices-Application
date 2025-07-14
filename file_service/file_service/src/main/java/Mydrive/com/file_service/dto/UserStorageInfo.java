package Mydrive.com.file_service.dto;

import lombok.Data;

@Data
public class UserStorageInfo {

    private Long userid;

    private Long leftallocatedstorageBytes;

    private Long usedstorageBytes;
}
