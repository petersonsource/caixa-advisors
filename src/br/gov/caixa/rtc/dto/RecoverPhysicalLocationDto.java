package br.gov.caixa.rtc.dto;

public class RecoverPhysicalLocationDto {

    private String centroCustoPai = null;
    private boolean paiCentralizado = false;
  
    
    
    public String getCentroCustoPai() {
        return centroCustoPai;
    }
    public void setCentroCustoPai(String centroCustoPai) {
        this.centroCustoPai = centroCustoPai;
    }
    public boolean isPaiCentralizado() {
        return paiCentralizado;
    }
    public void setPaiCentralizado(boolean paiCentralizado) {
        this.paiCentralizado = paiCentralizado;
    }
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((centroCustoPai == null) ? 0 : centroCustoPai.hashCode());
	result = prime * result + (paiCentralizado ? 1231 : 1237);
	return result;
    }
    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	RecoverPhysicalLocationDto other = (RecoverPhysicalLocationDto) obj;
	if (centroCustoPai == null) {
	    if (other.centroCustoPai != null)
		return false;
	} else if (!centroCustoPai.equals(other.centroCustoPai))
	    return false;
	if (paiCentralizado != other.paiCentralizado)
	    return false;
	return true;
    }
    
    
    
    
}
