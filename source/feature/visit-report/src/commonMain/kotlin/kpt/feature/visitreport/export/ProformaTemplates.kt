package kpt.feature.visitreport.export

import kpt.core.model.visit.ReportFormat
import kpt.core.model.visit.Visit

object ProformaTemplates {
    fun injectProformaFields(
        visit: Visit,
        drawHeading: (String) -> Unit,
        drawField: (String, String) -> Unit,
        drawTable: ((headers: List<String>, rows: List<List<String>>, colWeights: List<Float>?) -> Unit)? = null
    ) {
        val line = "...................................................."
        val shortLine = "........................"

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
            ReportFormat.PRE_SANCTION_UPTO_50L -> {
                drawHeading("ANNEXURE I-A - PRE-SANCTION INSPECTION (UPTO ₹ 50 LAKH)")
                drawField("Proforma Format", "PRE-SANCTION INSPECTION/ VERIFICATION REPORT FOR BUSINESS LOANS UP TO ₹ 50 LAKH (Fresh/Enhancement/Additional)")
                drawField("Name of the Borrower", fill(visit.customerName) + if (visit.loanAccountNumber.isNotBlank()) " (A/C: ${visit.loanAccountNumber})" else "")
                drawField("Branch / Office", fill(visit.branch))
                drawField("Address Residential", fill(visit.address))
                drawField("Registered Office / Place of Work", line)
                drawField("Activity of the Unit (Industry / Sector)", fill(visit.purpose))
                drawField("Details of Unit / Location visited", line)
                drawField("Person & Designation contacted", line)

                drawHeading("Details of facilities sought from our Bank (Amt in ₹ lakh)")
                if (drawTable != null) {
                    val facilitiesHeaders = listOf("Type of facilities", "Existing Limit, if any", "Proposed Limit")
                    val facilitiesRows = listOf(
                        listOf("Term Loan", line, line),
                        listOf("Working Capital", line, line),
                        listOf("Non-Fund", line, line),
                        listOf("TOTAL", line, line)
                    )
                    drawTable(facilitiesHeaders, facilitiesRows, listOf(1.5f, 1.2f, 1.2f))
                } else {
                    drawField("Facilities - Term Loan", "Existing: $shortLine | Proposed: $shortLine")
                    drawField("Facilities - Working Capital", "Existing: $shortLine | Proposed: $shortLine")
                    drawField("Facilities - Non-Fund", "Existing: $shortLine | Proposed: $shortLine")
                    drawField("Facilities - TOTAL", "Existing: $shortLine | Proposed: $shortLine")
                }

                drawHeading("Unit Assessment & Verification Details")
                drawField("Details of products traded / dealing with", line)
                drawField("Operation of unit satisfactory (Existing Unit)", line)
                drawField("Stock/Plant/Machinery as per financials submitted", line)
                drawField("Where Loan is secured by IP (Annexure I-A1)", line)
                drawField("Adverse feature (If any)", line)
                drawField("Any other observation", fill(visit.observations.ifBlank { visit.remarks }))

                drawHeading("Credentials & Antecedents Verification")
                drawField("Market Verification Note", "Credentials & Antecedents* of Borrower(s)/ Promoters / Guarantors verified from at least two persons and found satisfactory (*Not Applicable for Govt/PSU/Listed Cos)")
                if (drawTable != null) {
                    val antecedentHeaders = listOf("Name of the Person", "Name of the Firm / Company", "Relation / Connection with Borrower")
                    val antecedentRows = listOf(
                        listOf("1. $line", line, line),
                        listOf("2. $line", line, line)
                    )
                    drawTable(antecedentHeaders, antecedentRows, listOf(1.3f, 1.4f, 1.3f))
                } else {
                    drawField("Person 1 Verification", "Name: $shortLine | Firm: $shortLine | Relation: $shortLine")
                    drawField("Person 2 Verification", "Name: $shortLine | Firm: $shortLine | Relation: $shortLine")
                }

                drawHeading("Visiting Official Sign-Off")
                if (drawTable != null) {
                    val signHeaders = listOf("Official Particulars", "Details & Confirmation")
                    val signRows = listOf(
                        listOf("Signature of Visiting Official", "_______________________________"),
                        listOf("Visiting Official Name", fill(visit.officerName)),
                        listOf("Designation & PF No.", "Designation: $shortLine | PF No: $shortLine"),
                        listOf("Place & Date of Inspection", "Place: ${fill(visit.branch)} | Date: ${fill(visit.visitDate)}")
                    )
                    drawTable(signHeaders, signRows, listOf(1.2f, 2.5f))
                } else {
                    drawField("Visiting Official Name", fill(visit.officerName))
                    drawField("Designation & PF No.", "Designation: $shortLine | PF No: $shortLine")
                    drawField("Place & Date of Inspection", "Place: ${fill(visit.branch)} | Date: ${fill(visit.visitDate)}")
                    drawField("Signature", "_______________________________")
                }
            }
            ReportFormat.POST_SANCTION_UPTO_50L -> {
                drawHeading("ANNEXURE I-C - POST-SANCTION (UPTO ₹ 50 LAKH)")
                drawField("Proforma Format", "POST-SANCTION INSPECTION / VISIT REPORT FOR BUSINESS LOANS UPTO ₹ 50 LAKH (Renewal/ Review/ Consecutive Visit)")
                drawField("Name of the Borrower", fill(visit.customerName) + if (visit.loanAccountNumber.isNotBlank()) " (A/C: ${visit.loanAccountNumber})" else "")
                drawField("Branch / Office", fill(visit.branch))
                drawField("Address Residential", fill(visit.address))
                drawField("Registered Office / Place of Work", line)
                drawField("Activity of the Unit (Industry / Sector)", fill(visit.purpose))
                drawField("Details of Unit / Location visited", line)
                drawField("Person & Designation contacted", line)

                drawHeading("Details of facilities from our Bank (Amt in ₹ lakh)")
                if (drawTable != null) {
                    val facilitiesHeaders = listOf("Type of facilities", "Sanctioned Limit", "Outstanding Balance", "Status of Account")
                    val facilitiesRows = listOf(
                        listOf("Term Loan", line, line, line),
                        listOf("Working Capital", line, line, line),
                        listOf("Non-Fund", line, line, line),
                        listOf("TOTAL", line, line, line)
                    )
                    drawTable(facilitiesHeaders, facilitiesRows, listOf(1.3f, 1.1f, 1.1f, 1.1f))
                } else {
                    drawField("Facilities - Term Loan", line)
                    drawField("Facilities - Working Capital", line)
                    drawField("Facilities - Non-Fund", line)
                    drawField("Facilities - TOTAL", line)
                }

                drawHeading("Securities & Insurance Details")
                drawField("Assets created out of Bank finance", line)
                drawField("Securities insured with 'Agreed Bank Clause'", line)
                if (drawTable != null) {
                    val insHeaders = listOf("Insurance Detail", "Value of Insurance (₹ in Lakh)", "Valid Upto")
                    val insRows = listOf(
                        listOf("Fixed Assets", line, line),
                        listOf("Inventory", line, line)
                    )
                    drawTable(insHeaders, insRows, listOf(1.4f, 1.4f, 1.2f))
                } else {
                    drawField("Insurance - Fixed Assets", line)
                    drawField("Insurance - Inventory", line)
                }
                drawField("Whether Loan is secured by IP (Annexure I-C1)", line)
                drawField("Adverse Remarks, if any", line)
                drawField("Other observations in brief", fill(visit.observations.ifBlank { visit.remarks }))

                drawHeading("Visiting Official Sign-Off")
                if (drawTable != null) {
                    val signHeaders = listOf("Official Particulars", "Details & Confirmation")
                    val signRows = listOf(
                        listOf("Signature of Visiting Official", "_______________________________"),
                        listOf("Visiting Official Name", fill(visit.officerName)),
                        listOf("Designation & PF No.", "Designation: $shortLine | PF No: $shortLine"),
                        listOf("Place & Date of Inspection", "Place: ${fill(visit.branch)} | Date: ${fill(visit.visitDate)}")
                    )
                    drawTable(signHeaders, signRows, listOf(1.2f, 2.5f))
                } else {
                    drawField("Visiting Official Name", fill(visit.officerName))
                    drawField("Designation & PF No.", "Designation: $shortLine | PF No: $shortLine")
                    drawField("Place & Date of Inspection", "Place: ${fill(visit.branch)} | Date: ${fill(visit.visitDate)}")
                    drawField("Signature", "_______________________________")
                }
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
