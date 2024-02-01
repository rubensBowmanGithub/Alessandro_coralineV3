define({"topics" : [{"title":"Create a Pipeline and Define Pipeline Properties","shortdesc":"\n               <p class=\"shortdesc\">When you configure a pipeline, you need to decide what to do with error records. You         can discard them or - more productively\n                  - write them to file, another pipeline, or to Kafka. \n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_jmz_3dn_ls","attributes": {"data-id":"task_jmz_3dn_ls",},"menu": {"hasChildren":false,},"tocID":"task_jmz_3dn_ls-d46e203257","topics":[]},{"title":"Configure the Origin","shortdesc":"\n               <p class=\"shortdesc\">The origin represents the incoming data for the pipeline. When you configure the         origin, you define how to connect\n                  to the origin system, the type of data to be processed,         and other properties specific to the origin.\n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_ftt_2vq_ks","attributes": {"data-id":"task_ftt_2vq_ks",},"menu": {"hasChildren":false,},"tocID":"task_ftt_2vq_ks-d46e203282","topics":[]},{"title":"Preview Data ","shortdesc":"\n               <p class=\"shortdesc\">To become more familiar with the data set and gather some important details for         pipeline configuration, let&apos;s preview\n                  the source data.\n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_xb5_2sf_4s","attributes": {"data-id":"task_xb5_2sf_4s",},"menu": {"hasChildren":false,},"tocID":"task_xb5_2sf_4s-d46e203317","topics":[]},{"title":"Route Data with the Stream Selector","shortdesc":"\n               <p class=\"shortdesc\">To route data to different streams for processing, we use the Stream Selector         processor.</p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_br5_rzq_ks","attributes": {"data-id":"task_br5_rzq_ks",},"menu": {"hasChildren":false,},"tocID":"task_br5_rzq_ks-d46e203362","topics":[]},{"title":"Use Jython for Card Typing","shortdesc":"\n               <p class=\"shortdesc\"> Next, we&apos;ll evaluate credit card numbers to determine the credit card type. You can         use an Expression Evaluator to\n                  do the same calculations, but with a short script, the Jython         Evaluator is easier.\n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_cdx_kfm_ls","attributes": {"data-id":"task_cdx_kfm_ls",},"menu": {"hasChildren":false,},"tocID":"task_cdx_kfm_ls-d46e203417","topics":[]},{"title":"Mask Credit Card Numbers","shortdesc":"\n               <p class=\"shortdesc\"> Now let&apos;s prevent sensitive information from reaching internal databases by using a         Field Masker to mask the credit\n                  card numbers. \n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_qbw_d3m_ls","attributes": {"data-id":"task_qbw_d3m_ls",},"menu": {"hasChildren":false,},"tocID":"task_qbw_d3m_ls-d46e203483","topics":[]},{"title":"Write to the Destination","shortdesc":"\n               <p class=\"shortdesc\">The <span class=\"ph\">Data Collector</span> can         write data to many destinations. The Local FS destination writes to files in a local file         system. \n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_wsj_5tm_ls","attributes": {"data-id":"task_wsj_5tm_ls",},"menu": {"hasChildren":false,},"tocID":"task_wsj_5tm_ls-d46e203559","topics":[]},{"title":"Add a Corresponding Field with the Expression Evaluator","shortdesc":"\n               <p class=\"shortdesc\">The Jython Evaluator script added a new field to the credit payments branch. To         ensure all records have the same structure,\n                  we&apos;ll use the Expression Evaluator to add the         same field to the non-credit branch. \n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_wgb_t4p_ms","attributes": {"data-id":"task_wgb_t4p_ms",},"menu": {"hasChildren":false,},"tocID":"task_wgb_t4p_ms-d46e203648","topics":[]},{"title":"Create a Data Rule and Alert","shortdesc":"\n               <p class=\"shortdesc\">Now before we run the basic pipeline, let&apos;s add a data rule and alert. Data rules are         user-defined rules used to inspect\n                  data moving between two stages. They are a powerful way         to look for outliers and anomalous data. \n               </p>\n            ","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_ets_dkj_ps","attributes": {"data-id":"task_ets_dkj_ps",},"menu": {"hasChildren":false,},"tocID":"task_ets_dkj_ps-d46e203744","topics":[]},{"title":"Run the Basic Pipeline","href":"datacollector\/UserGuide\/Tutorial\/BasicTutorial.html#task_m21_dyj_ps","attributes": {"data-id":"task_m21_dyj_ps",},"menu": {"hasChildren":false,},"tocID":"task_m21_dyj_ps-d46e203850","topics":[]}]});