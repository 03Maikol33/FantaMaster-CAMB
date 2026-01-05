package it.camb.fantamaster.model;

public class BidResult {
    private int rosaId;
    private int offerta;
    private String nomeRosa;

    public BidResult(int rosaId, int offerta, String nomeRosa) {
        this.rosaId = rosaId;
        this.offerta = offerta;
        this.nomeRosa = nomeRosa;
    }
    
    public int getRosaId() {
        return rosaId;
    }
    public int getOfferta() {
        return offerta;
    }
    public String getNomeRosa() {
        return nomeRosa;
    }
    
}