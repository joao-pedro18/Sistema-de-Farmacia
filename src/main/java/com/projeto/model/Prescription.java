package model;

public class Prescription {
    //Atributos da classe

    private String number;
    private String doctor;

    //Construtor da classe

    public Prescription (String number, String doctor){
        this.number = number;
        this.doctor = doctor;
    }

    //Metodos getters

    public String getNumber(){
        return number;
    }
    public String getDoctor(){
        return doctor;
    }


}
