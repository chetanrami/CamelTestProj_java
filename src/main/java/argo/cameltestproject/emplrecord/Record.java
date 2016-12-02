package argo.cameltestproject.emplrecord;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "row", namespace = "http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_QUERYRESULTS_XMLP_RESP.VERSION_1")
@CsvRecord(quoting = true, crlf = "UNIX", generateHeaderColumns = true, separator = ",")
@ToString
@EqualsAndHashCode
@Data
public class Record implements Serializable {
    @XmlAttribute(name = "rownumber")
    private int rowNumber;

    @XmlElement(name = "EMPLID", namespace = "http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_QUERYRESULTS_XMLP_RESP.VERSION_1")
    @DataField(pos = 2, columnName = "EMPLOYEE_ID")
    private String employeeId;

    @XmlElement(name = "NAME", namespace = "http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_QUERYRESULTS_XMLP_RESP.VERSION_1")
    @DataField(pos = 1, columnName = "EMPLOYEE_NAME")
    private String employeeName;

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
