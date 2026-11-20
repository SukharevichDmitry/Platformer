sf 
- master-detail у major_Capex
Чтобы его удалить нужно Убрать его во всех Apex классах и с Page Layout

workOrderDetails.js
- + getExistingServicePricingInputs

majorCapexItem.js
- + connectedCallback() this.serviceInput = this.capexData.Service_Pricing_Input__c ?? 0;
- + getCapexData() capexData.Service_Pricing_Input__c = this.serviceInput ?? 0;

workOrder.js
- + getWorkOrderData()
- + sPNoPRCalculation()         const Service_Pricing_Input__c = this.existingData.Id;
- + handleAddMajorCapexItem() Service_Pricing_Input__c: this.existingData.Id
- + async handleFieldChange(event) servicePricingInputId: typeof this.existingData.Id === 'string' ? this.existingData.Id : null
- + loadResults() this.results = await getPricingLinesForServicePricingInputs({ servicePricingInputId: this.existingData.Id});

WorkOrderDetailsController.cls
- + getExistingServicePricingInputs(        Id opportunityId, Id servicePricingInputId)
- + getMajorCapexForServicePricingInputs(Id servicePricingInputId)
- + getPricingLinesForServicePricingInputs(Id servicePricingInputId)
- + saveWorkOrders(List< Object> items)
- + deleteWorkOrder(Id workOrderId)
