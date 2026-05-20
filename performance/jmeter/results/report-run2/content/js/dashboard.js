/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 99.57274086733604, "KoPercent": 0.4272591326639607};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.9816278572954497, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.125, 500, 1500, "Post Refund"], "isController": false}, {"data": [0.8741258741258742, 500, 1500, "Post Withdraw"], "isController": false}, {"data": [0.13157894736842105, 500, 1500, "Post decuct"], "isController": false}, {"data": [0.9736842105263158, 500, 1500, "Post Check Balance"], "isController": false}, {"data": [0.9809688581314879, 500, 1500, "Post Topup"], "isController": false}, {"data": [0.9913194444444444, 500, 1500, "Post Pay"], "isController": false}, {"data": [0.9856957087126138, 500, 1500, "Get Transaction"], "isController": false}, {"data": [0.9931572246976448, 500, 1500, "Get Wallet"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 4681, 20, 0.4272591326639607, 235.41059602649028, 2, 61422, 9.0, 280.0, 353.89999999999964, 1610.9800000000178, 47.6607442854961, 70.09448738100086, 19.532005676322353], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["Post Refund", 12, 0, 0.0, 15025.916666666664, 1209, 61422, 2688.5, 60193.50000000001, 61422.0, 61422.0, 0.12945123464115038, 0.0323733434287317, 0.07049867042794421], "isController": false}, {"data": ["Post Withdraw", 143, 16, 11.188811188811188, 278.5524475524476, 6, 29832, 18.0, 294.3999999999999, 364.4, 16987.080000000067, 1.516935577973671, 0.4261616027007818, 0.7243752751434723], "isController": false}, {"data": ["Post decuct", 19, 1, 5.2631578947368425, 8366.684210526315, 777, 59749, 1807.0, 26176.0, 59749.0, 59749.0, 0.1956987475280158, 0.06711050799274886, 0.1009373422822594], "isController": false}, {"data": ["Post Check Balance", 19, 0, 0.0, 84.68421052631581, 3, 1022, 5.0, 224.0, 1022.0, 1022.0, 0.4948818795092856, 0.12132949040189618, 0.2308058272341312], "isController": false}, {"data": ["Post Topup", 289, 1, 0.3460207612456747, 232.24567474048447, 5, 23522, 14.0, 338.0, 391.5, 2757.5000000004275, 3.0271606490064835, 0.880071345148687, 1.3422632608490714], "isController": false}, {"data": ["Post Pay", 288, 1, 0.3472222222222222, 184.55555555555554, 6, 30328, 15.0, 322.0, 361.75000000000006, 815.030000000001, 3.025273640202525, 0.8614139247935881, 1.5921893546870733], "isController": false}, {"data": ["Get Transaction", 769, 1, 0.13003901170351106, 216.19895968790644, 6, 30637, 17.0, 348.0, 388.0, 1097.8999999999985, 7.914291007142415, 59.37642917468559, 3.207451921839944], "isController": false}, {"data": ["Get Wallet", 3142, 0, 0.0, 138.35423297262872, 2, 29666, 5.0, 203.40000000000055, 309.8499999999999, 425.1400000000003, 31.991040065163165, 9.108455063508629, 12.558982525581632], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["400", 16, 80.0, 0.34180730613116855], "isController": false}, {"data": ["500", 2, 10.0, 0.04272591326639607], "isController": false}, {"data": ["Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 2, 10.0, 0.04272591326639607], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 4681, 20, "400", 16, "500", 2, "Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 2, "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": ["Post Withdraw", 143, 16, "400", 16, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["Post decuct", 19, 1, "Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": ["Post Topup", 289, 1, "Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["Post Pay", 288, 1, "500", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["Get Transaction", 769, 1, "500", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
