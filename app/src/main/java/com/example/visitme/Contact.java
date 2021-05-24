package com.example.visitme;

public class  Contact {
    private String fullName;
    private String company;
    private String email;
    private String webSite;
    private String profession;
    private String mobile1;
    private String mobile2;
    private String address;
    private String fix1;
    private String fix2;
    private String image;
    private String category;


    public Contact(String fullName, String company, String email, String webSite, String profession, String mobile1, String mobile2, String address, String fix1, String fix2, String image, String category) {
        this.fullName = fullName;
        this.company = company;
        this.email = email;
        this.webSite = webSite;
        this.profession = profession;
        this.mobile1 = mobile1;
        this.mobile2 = mobile2;
        this.address = address;
        this.fix1 = fix1;
        this.fix2 = fix2;
        this.image = image;
        this.category =category;
    }

    Contact()
    {

    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebSite() {
        return webSite;
    }

    public void setWebSite(String webSite) {
        this.webSite = webSite;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getMobile1() {
        return mobile1;
    }

    public void setMobile1(String mobile1) {
        this.mobile1 = mobile1;
    }

    public String getMobile2() {
        return mobile2;
    }

    public void setMobile2(String mobile2) {
        this.mobile2 = mobile2;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFix1() {
        return fix1;
    }

    public void setFix1(String fix1) {
        this.fix1 = fix1;
    }

    public String getFix2() {
        return fix2;
    }

    public void setFix2(String fix2) {
        this.fix2 = fix2;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "fullName='" + fullName + '\'' +
                ", company='" + company + '\'' +
                ", email='" + email + '\'' +
                ", webSite='" + webSite + '\'' +
                ", profession='" + profession + '\'' +
                ", mobile1='" + mobile1 + '\'' +
                ", mobile2='" + mobile2 + '\'' +
                ", address='" + address + '\'' +
                ", fix1='" + fix1 + '\'' +
                ", fix2='" + fix2 + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
