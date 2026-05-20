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

    var data = {"OkPercent": 99.56687898089172, "KoPercent": 0.43312101910828027};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.981656050955414, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.22727272727272727, 500, 1500, "Post Refund"], "isController": false}, {"data": [0.8939393939393939, 500, 1500, "Post Withdraw"], "isController": false}, {"data": [0.125, 500, 1500, "Post decuct"], "isController": false}, {"data": [1.0, 500, 1500, "Post Check Balance"], "isController": false}, {"data": [0.9886792452830189, 500, 1500, "Post Topup"], "isController": false}, {"data": [0.9943181818181818, 500, 1500, "Post Pay"], "isController": false}, {"data": [0.9865293185419969, 500, 1500, "Get Transaction"], "isController": false}, {"data": [0.9913093858632677, 500, 1500, "Get Wallet"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 3925, 17, 0.43312101910828027, 225.1640764331208, 2, 65103, 8.0, 192.0, 334.6999999999998, 1821.0, 43.071756998474655, 61.9983070501882, 17.709562873105668], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["Post Refund", 11, 0, 0.0, 9591.0, 809, 55253, 1699.0, 50991.400000000016, 55253.0, 55253.0, 0.1251308185830641, 0.0314160116257906, 0.06814208462255995], "isController": false}, {"data": ["Post Withdraw", 132, 13, 9.848484848484848, 249.9469696969697, 7, 24586, 12.0, 178.80000000000018, 352.35, 17073.549999999716, 1.5057148723564437, 0.4240281473148085, 0.7188928539855817], "isController": false}, {"data": ["Post decuct", 16, 0, 0.0, 11431.1875, 804, 65103, 2039.0, 44800.90000000002, 65103.0, 65103.0, 0.178649188821027, 0.044378796295262445, 0.09727352377150768], "isController": false}, {"data": ["Post Check Balance", 17, 0, 0.0, 50.64705882352941, 3, 423, 5.0, 230.99999999999983, 423.0, 423.0, 0.6831973636619378, 0.16746341628822892, 0.3187181635052044], "isController": false}, {"data": ["Post Topup", 265, 0, 0.0, 242.59245283018868, 6, 25478, 11.0, 272.20000000000005, 344.9999999999999, 8414.719999999426, 3.005727896557591, 0.8561273606306357, 1.3375356221289627], "isController": false}, {"data": ["Post Pay", 264, 0, 0.0, 64.4090909090909, 6, 1821, 11.0, 229.0, 344.75, 474.200000000003, 4.28905640759033, 1.2221944961171043, 2.257047394885625], "isController": false}, {"data": ["Get Transaction", 631, 2, 0.31695721077654515, 227.08557844690972, 8, 25587, 15.0, 294.0, 354.4, 1462.039999999996, 7.158577814081184, 53.457828489097636, 2.8965836864406778], "isController": false}, {"data": ["Get Wallet", 2589, 2, 0.0772499034376207, 130.14020857473926, 2, 25219, 5.0, 78.0, 290.5, 609.0999999999999, 29.224517439891635, 8.34000248687775, 11.468474856078563], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["400", 12, 70.58823529411765, 0.3057324840764331], "isController": false}, {"data": ["500", 3, 17.647058823529413, 0.07643312101910828], "isController": false}, {"data": ["Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 2, 11.764705882352942, 0.050955414012738856], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 3925, 17, "400", 12, "500", 3, "Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 2, "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": ["Post Withdraw", 132, 13, "400", 12, "500", 1, "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["Get Transaction", 631, 2, "500", 1, "Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 1, "", "", "", "", "", ""], "isController": false}, {"data": ["Get Wallet", 2589, 2, "500", 1, "Non HTTP response code: org.apache.http.NoHttpResponseException/Non HTTP response message: localhost:6002 failed to respond", 1, "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
