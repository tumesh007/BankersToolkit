package kpt.feature.visitreport.export

import kpt.core.model.visit.ReportFormat
import kpt.core.model.visit.Visit

object ProformaTemplates {
    fun injectProformaFields(
        visit: Visit,
        drawHeading: (String) -> Unit,
        drawField: (String, String) -> Unit
    ) {
        val line = "...................................................."
        
        fun fill(v: String?): String {
            return if (v.isNullOrBlank()) line else v
        }

        when (visit.reportFormat) {
            ReportFormat.GENERAL -> {
                // Nothing extra
            }
            ReportFormat.PART_A -> {
                drawHeading("PART A - INSPECTION OF STOCKS")
                drawField("Customer Name", fill(visit.customerName))
                drawField("Premises owned/rented", line)
                drawField("Bank's name plate displayed", line)
                drawField("Stock records maintained", line)
                drawField("System for valuation", line)
                drawField("Old/obsolete stocks", line)
                drawField("General working", line)
                drawField("Labour situation", line)
                drawField("Date of stock statement", line)
                drawField("Drawing Power", line)
                drawField("Value of stock on visit", line)
                drawField("Variance observed", line)
            }
            ReportFormat.PART_B -> {
                drawHeading("PART B - PROJECT (IMPLEMENTATION)")
                drawField("Customer Name", fill(visit.customerName))
                drawField("Location of factory", fill(visit.address))
                drawField("Brief particulars", line)
                drawField("Quarter of progress", line)
                drawField("Date of inspection", fill(visit.visitDate))
                drawField("Statutory approvals", line)
                drawField("Cost of project", line)
                drawField("Means of finance", line)
                drawField("Progress details", line)
                drawField("Time overrun", line)
                drawField("Stage of completion", line)
            }
            ReportFormat.PART_C -> {
                drawHeading("PART C - PROJECT (COMMERCIAL)")
                drawField("Customer Name", fill(visit.customerName))
                drawField("Location of factory", fill(visit.address))
                drawField("Brief particulars", line)
                drawField("Statutory approvals", line)
                drawField("Physical/financial status", line)
                drawField("Order book position", line)
                drawField("Net fixed assets", line)
                drawField("Disbursement schedule", line)
                drawField("Availability period", line)
                drawField("Repayment obligations", line)
            }
            ReportFormat.PART_D -> {
                drawHeading("PART D - IMMOVABLE PROPERTY")
                drawField("Name of Owner", fill(visit.customerName))
                drawField("Ownership Type", line)
                drawField("Relation to Borrower", line)
                drawField("Address / Landmark", fill(visit.address))
                drawField("Type of Property", line)
                drawField("Purpose / Usage", line)
                drawField("Self-occupied / Rented", line)
                drawField("Independently accessible", line)
                drawField("Encroachment observed", line)
                drawField("Insurance in force", line)
                drawField("Notice of other Bank", line)
                drawField("Taxes paid", line)
                drawField("Utility bills name", line)
            }
            ReportFormat.POST_SANCTION_UPTO_50L -> {
                drawHeading("ANNEXURE I-C - POST-SANCTION (UPTO 50L)")
                drawField("Name of the Borrower", fill(visit.customerName) + if (!visit.loanAccountNumber.isNullOrBlank()) " (A/C: ${visit.loanAccountNumber})" else "")
                drawField("Branch/ Office", fill(visit.branch))
                drawField("Address Residential", fill(visit.address))
                drawField("Registered Office/Place of Work", line)
                drawField("Activity of the Unit (Industry / Sector)", line)
                drawField("Details of Unit / Location visited", line)
                drawField("Name of the person & designation contacted", line)
                drawField("Details of facilities sought (TL/WC/Non-fund)", line)
                drawField("Whether assets created out of Bank finance", line)
                drawField("Securities insured with 'Agreed Bank Clause'", line)
                drawField("Insurance Detail (Fixed assets / Inventory)", line)
                drawField("Value of insurance / Valid upto", line)
                drawField("Whether Loan is secured by IP", line)
                drawField("Adverse Remarks, if any", line)
                drawField("Other observations in brief", fill(visit.remarks))
            }
            ReportFormat.CASA_CURRENT_ACCOUNT -> {
                drawHeading("VISIT PERFORMA - CURRENT ACCOUNT (CASA)")
                drawField("Branch (BO) / D.No.", fill(visit.branch))
                drawField("Date of Visit", fill(visit.visitDate))
                drawField("Reference / Account Number", fill(visit.accountNumber))
                drawField("Name of Customer(s)", fill(visit.customerName))
                drawField("Residential Address of Beneficial Owner(s)", line)
                drawField("Business Place Address of the Customer", fill(visit.address))
                drawField("Business Place (Rented / Owned)", line)
                drawField("Date of Verification (Residential Address)", line)
                drawField("Date of Verification (Business Place Address)", line)
                drawField("Visiting Official Name", fill(visit.officerName))
                drawField("Official Designation", line)
                drawField("PF No. / GBPA No.", line)
                drawField("Mobile No.", line)
                drawHeading("Declaration")
                drawField("Confirmation", "I confirm the correctness of the residential address of the Beneficial Owner(s) and business place address of the customer.")
            }
        }
    }
}
