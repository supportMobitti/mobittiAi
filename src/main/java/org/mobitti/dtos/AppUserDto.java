package org.mobitti.dtos;

public class AppUserDto {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String roleName;
    private int clubId;
    private int superClubId;
    private String department;
    private String storageId;
    private String birthday;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
    public String getRoleName() {
        return roleName;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    public int getClubId() {
        return clubId;
    }
    public void setClubId(int clubId) {
        this.clubId = clubId;
    }
    public int getSuperClubId() {
        return superClubId;
    }
    public void setSuperClubId(int superClubId) {
        this.superClubId = superClubId;
    }
    public String getStorageId() {
        return storageId;
    }
    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public String getBirthday() {
        return birthday;
    }
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
}
